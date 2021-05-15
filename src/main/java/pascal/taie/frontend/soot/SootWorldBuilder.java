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

import pascal.taie.AbstractWorldBuilder;
import pascal.taie.World;
import pascal.taie.analysis.oldpta.env.Environment;
import pascal.taie.config.Options;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.ClassHierarchyImpl;
import pascal.taie.language.type.TypeManager;
import pascal.taie.language.type.TypeManagerImpl;
import pascal.taie.util.Timer;
import soot.G;
import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.Transform;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static soot.SootClass.HIERARCHY;

public class SootWorldBuilder extends AbstractWorldBuilder {

    /**
     * Only used by old pointer analysis. Will be deprecated after
     * removing old pointer analysis.
     */
    @Deprecated
    public SootWorldBuilder(Options options, Scene scene) {
        build(options, scene);
    }

    public SootWorldBuilder() {
    }

    @Override
    public void build(Options options) {
        runSoot(options, this);
    }

    private static void runSoot(Options options, SootWorldBuilder builder) {
        initSoot();
        // Set Soot options
        soot.options.Options.v().set_output_format(
                soot.options.Options.output_format_jimple);
        soot.options.Options.v().set_keep_line_number(true);
        soot.options.Options.v().set_whole_program(true);
        soot.options.Options.v().set_no_writeout_body_releasing(true);
        soot.options.Options.v().setPhaseOption("jb", "preserve-source-annotations:true");
        soot.options.Options.v().setPhaseOption("jb", "model-lambdametafactory:false");
        soot.options.Options.v().setPhaseOption("cg", "enabled:false");

        if (options.isPrependJVM()) {
            // TODO: figure out why -prepend-classpath makes Soot faster
            soot.options.Options.v().set_prepend_classpath(true);
        }

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

    public static void initSoot() {
        G.reset();
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
        Environment env = new Environment();
        world.setEnvironment(env);
        IRBuilder irBuilder = new IRBuilder(converter, env);
        world.setIRBuilder(irBuilder);
        if (options.isPreBuildIR()) {
            irBuilder.buildAll(hierarchy);
        }
    }

    protected static void buildClasses(ClassHierarchy hierarchy, Scene scene) {
        // Parallelize?
        Timer timer = new Timer("Build all classes");
        timer.start();
        new ArrayList<>(scene.getClasses()).forEach(c ->
                hierarchy.getDefaultClassLoader().loadClass(c.getName()));
        timer.stop();
        System.out.println(timer);
        System.out.println("#classes: " + hierarchy.getAllClasses().size());
        System.out.println("#methods: " + hierarchy.getAllClasses()
                .stream()
                .mapToInt(c -> c.getDeclaredMethods().size())
                .sum());
    }
}
