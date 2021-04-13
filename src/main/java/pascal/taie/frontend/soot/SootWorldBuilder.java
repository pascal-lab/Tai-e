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

import pascal.taie.Options;
import pascal.taie.World;
import pascal.taie.WorldBuilder;
import pascal.taie.analysis.oldpta.env.Environment;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.ClassHierarchyImpl;
import pascal.taie.language.natives.DefaultNativeModel;
import pascal.taie.language.natives.EmptyNativeModel;
import pascal.taie.language.natives.NativeModel;
import pascal.taie.language.types.TypeManager;
import pascal.taie.language.types.TypeManagerImpl;
import pascal.taie.util.Timer;
import soot.G;
import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.Transform;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static soot.SootClass.HIERARCHY;

public class SootWorldBuilder implements WorldBuilder {

    private static final String JREs = "java-benchmarks/JREs";

    private static final List<String> implicitEntries = List.of(
            "<java.lang.System: void initializeSystemClass()>",
            "<java.lang.Thread: void <init>(java.lang.ThreadGroup,java.lang.Runnable)>",
            "<java.lang.Thread: void <init>(java.lang.ThreadGroup,java.lang.String)>",
            "<java.lang.ThreadGroup: void <init>()>",
            "<java.lang.ThreadGroup: void <init>(java.lang.ThreadGroup,java.lang.String)>",
            "<java.lang.Thread: void exit()>",
            "<java.lang.ThreadGroup: void uncaughtException(java.lang.Thread,java.lang.Throwable)>",
            "<java.lang.ClassLoader: void <init>()>",
            "<java.lang.ClassLoader: java.lang.Class loadClassInternal(java.lang.String)>",
            "<java.lang.ClassLoader: void checkPackageAccess(java.lang.Class,java.security.ProtectionDomain)>",
            "<java.lang.ClassLoader: void addClass(java.lang.Class)>",
            "<java.lang.ClassLoader: long findNative(java.lang.ClassLoader,java.lang.String)>",
            "<java.security.PrivilegedActionException: void <init>(java.lang.Exception)>"
    );

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
        // when running soot with java 1.8
        scene.addBasicClass("java.awt.dnd.MouseDragGestureRecognizer", HIERARCHY);
        scene.addBasicClass("java.lang.annotation.Inherited", HIERARCHY);
        scene.addBasicClass("javax.crypto.spec.IvParameterSpec", HIERARCHY);
        scene.addBasicClass("javax.sound.sampled.Port", HIERARCHY);
        scene.addBasicClass("sun.util.locale.provider.HostLocaleProviderAdapterImpl", HIERARCHY);

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

    private static String getClassPath(Options options) {
        if (options.isPrependJVM()) {
            return options.getClassPath();
        } else { // when prependJVM is not set, we manually specify JRE jars
            String jrePath = String.format("%s/jre1.%d",
                    JREs, options.getJavaVersion());
            try (Stream<Path> paths = Files.walk(Paths.get(jrePath))) {
                return Stream.concat(
                        paths.map(Path::toString).filter(p -> p.endsWith(".jar")),
                        Stream.of(options.getClassPath()))
                        .collect(Collectors.joining(File.pathSeparator));
            } catch (IOException e) {
                throw new RuntimeException("Analysis on Java " + options.getJavaVersion() +
                        " is not supported yet");
            }
        }
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

    private static void buildClasses(ClassHierarchy hierarchy, Scene scene) {
        // Parallelize?
        Timer timer = new Timer("Build all classes");
        timer.start();
        scene.getClasses().forEach(c ->
                hierarchy.getDefaultClassLoader().loadClass(c.getName()));
        timer.stop();
        System.out.println(timer);
        if (World.getOptions().isDumpClasses()) {
            ClassDumper dumper = new ClassDumper();
            scene.getClasses().forEach(dumper::dump);
        }
        System.out.println("#classes: " + hierarchy.getAllClasses().size());
        System.out.println("#methods: " + hierarchy.getAllClasses()
                .stream()
                .mapToInt(c -> c.getDeclaredMethods().size())
                .sum());
    }

    private static NativeModel getNativeModel(
            TypeManager typeManager, ClassHierarchy hierarchy) {
        return World.getOptions().enableNativeModel() ?
                new DefaultNativeModel(typeManager, hierarchy) :
                new EmptyNativeModel(typeManager, hierarchy);
    }
}
