/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2020-- Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020-- Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * Tai-e is only for educational and academic purposes,
 * and any form of commercial use is disallowed.
 * Distribution of Tai-e is disallowed without the approval.
 */

package pascal.taie.frontend.soot;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.AbstractWorldBuilder;
import pascal.taie.World;
import pascal.taie.analysis.pta.PointerAnalysis;
import pascal.taie.analysis.pta.plugin.reflection.LogItem;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.config.Options;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.ClassHierarchyImpl;
import pascal.taie.language.classes.StringReps;
import pascal.taie.language.type.TypeManager;
import pascal.taie.language.type.TypeManagerImpl;
import pascal.taie.util.Timer;
import soot.G;
import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.Transform;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static soot.SootClass.HIERARCHY;

public class SootWorldBuilder extends AbstractWorldBuilder {

    private static final Logger logger = LogManager.getLogger(SootWorldBuilder.class);

    @Override
    public void build(Options options, List<AnalysisConfig> plan) {
        runSoot(options, plan, this);
    }

    private static void runSoot(Options options, List<AnalysisConfig> plan,
                                SootWorldBuilder builder) {
        initSoot(options, plan);
        // Configure Soot transformer
        Transform transform = new Transform(
                "wjtp.tai-e", new SceneTransformer() {
            @Override
            protected void internalTransform(String phaseName, Map<String, String> opts) {
                builder.build(options, Scene.v());
            }
        });
        PackManager.v()
                .getPack("wjtp")
                .add(transform);

        // Run main analysis
        soot.Main.main(new String[]{"-cp", getClassPath(options),
                options.getMainClass()});
    }

    public static void initSoot(Options options, List<AnalysisConfig> plan) {
        G.reset();

        // set Soot options
        soot.options.Options.v().set_output_format(
                soot.options.Options.output_format_jimple);
        soot.options.Options.v().set_keep_line_number(true);
        soot.options.Options.v().set_app(true);
        // exclude jdk.* from application classes
        soot.options.Options.v().set_exclude(List.of("jdk.*"));
        soot.options.Options.v().set_whole_program(true);
        soot.options.Options.v().set_no_writeout_body_releasing(true);
        soot.options.Options.v().setPhaseOption("jb", "preserve-source-annotations:true");
        soot.options.Options.v().setPhaseOption("jb", "model-lambdametafactory:false");
        soot.options.Options.v().setPhaseOption("cg", "enabled:false");
        if (options.isPrependJVM()) {
            // TODO: figure out why -prepend-classpath makes Soot faster
            soot.options.Options.v().set_prepend_classpath(true);
        }

        Scene scene = G.v().soot_Scene();
        // The following line is necessary to avoid a runtime exception
        // when running soot with Java 1.8
        scene.addBasicClass("java.awt.dnd.MouseDragGestureRecognizer", HIERARCHY);
        scene.addBasicClass("java.lang.annotation.Inherited", HIERARCHY);
        scene.addBasicClass("javax.crypto.spec.IvParameterSpec", HIERARCHY);
        scene.addBasicClass("javax.sound.sampled.Port", HIERARCHY);
        scene.addBasicClass("sun.util.locale.provider.HostLocaleProviderAdapterImpl", HIERARCHY);

        // Necessary for Java 11
        scene.addBasicClass("java.lang.invoke.VarHandleGuards", HIERARCHY);

        // TODO: avoid adding non-exist basic classes. This requires to
        //  check class path before adding these classes.
        // For simulating the FileSystem class, we need the implementation
        // of the FileSystem, but the classes are not loaded automatically
        // due to the indirection via native code.
        scene.addBasicClass("java.io.UnixFileSystem");
        scene.addBasicClass("java.io.WinNTFileSystem");
        scene.addBasicClass("java.io.Win32FileSystem");
        // java.net.URL loads handlers dynamically
        scene.addBasicClass("sun.net.www.protocol.file.Handler");
        scene.addBasicClass("sun.net.www.protocol.ftp.Handler");
        scene.addBasicClass("sun.net.www.protocol.http.Handler");
        // The following line caused SootClassNotFoundException
        // for sun.security.ssl.SSLSocketImpl. TODO: fix this
        // scene.addBasicClass("sun.net.www.protocol.https.Handler");
        scene.addBasicClass("sun.net.www.protocol.jar.Handler");

        addReflectionLogClasses(plan, scene);
    }

    /**
     * Add classes in reflection log to the scene.
     * Tai-e's ClassHierarchy depends on Soot's Scene, which does not change
     * after hierarchy's construction, thus we need to add the classes
     * in the reflection log before starting Soot.
     */
    private static void addReflectionLogClasses(List<AnalysisConfig> plan, Scene scene) {
        plan.forEach(config -> {
            if (config.getId().equals(PointerAnalysis.ID)) {
                String path = config.getOptions().getString("reflection-log");
                if (path != null) {
                    LogItem.load(path).forEach(item -> {
                        // add caller class
                        String caller = item.caller;
                        String callerClass = caller.substring(0, caller.lastIndexOf('.'));
                        scene.addBasicClass(callerClass);
                        // add target class
                        String target = item.target;
                        String targetClass;
                        if (target.startsWith("<")) {
                            targetClass = StringReps.getClassNameOf(target);
                        } else {
                            targetClass = target;
                        }
                        scene.addBasicClass(targetClass);
                    });
                }
            }
        });
    }

    private void build(Options options, Scene scene) {
        World.reset();
        World world = new World();
        World.set(world);

        // options will be used during World building, thus it should be
        // set at first.
        world.setOptions(options);
        // initialize class hierarchy
        ClassHierarchy hierarchy = new ClassHierarchyImpl();
        SootClassLoader loader = new SootClassLoader(scene, hierarchy);
        hierarchy.setDefaultClassLoader(loader);
        hierarchy.setBootstrapClassLoader(loader);
        world.setClassHierarchy(hierarchy);
        // initialize type manager
        TypeManager typeManager = new TypeManagerImpl(hierarchy);
        world.setTypeManager(typeManager);
        // initialize converter
        Converter converter = new Converter(loader, typeManager);
        loader.setConverter(converter);
        // build classes in hierarchy
        buildClasses(hierarchy, scene);
        // set main method
        if (scene.hasMainClass()) {
            world.setMainMethod(converter.convertMethod(scene.getMainMethod()));
        }
        // set implicit entries
        world.setImplicitEntries(implicitEntries.stream()
                .map(hierarchy::getJREMethod)
                // some implicit entries may not exist in certain JDK version,
                // thus we filter out null
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
        // initialize IR builder
        world.setNativeModel(getNativeModel(typeManager, hierarchy));
        IRBuilder irBuilder = new IRBuilder(converter);
        world.setIRBuilder(irBuilder);
        if (options.isPreBuildIR()) {
            irBuilder.buildAll(hierarchy);
        }
    }

    protected static void buildClasses(ClassHierarchy hierarchy, Scene scene) {
        // TODO: parallelize?
        Timer timer = new Timer("Build all classes");
        timer.start();
        new ArrayList<>(scene.getClasses()).forEach(c ->
                hierarchy.getDefaultClassLoader().loadClass(c.getName()));
        timer.stop();
        logger.info(timer);
        logger.info("#classes: {}", () -> hierarchy.allClasses().count());
        logger.info("#methods: {}", () -> hierarchy.allClasses()
                .mapToInt(c -> c.getDeclaredMethods().size()).sum());
    }
}
