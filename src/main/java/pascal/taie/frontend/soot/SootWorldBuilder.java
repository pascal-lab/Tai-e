/*
 * Tai-e - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Tai-e is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.taie.frontend.soot;

import pascal.taie.java.ClassHierarchy;
import pascal.taie.java.TypeManager;
import pascal.taie.java.World;
import pascal.taie.java.WorldBuilder;
import pascal.taie.java.classes.ClassHierarchyImpl;
import pascal.taie.java.types.TypeManagerImpl;
import soot.Scene;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static soot.SootClass.HIERARCHY;

public class SootWorldBuilder implements WorldBuilder {

    private static final List<String> implicitEntries = Arrays.asList(
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

    @Override
    public World build() {
        World world = new World();
        // initialize class hierarchy
        ClassHierarchy hierarchy = new ClassHierarchyImpl();
        Scene scene = Scene.v();
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
        World.set(world); // <-- order?
        // build classes in hierarchy
        initSoot(scene);
        buildClasses(hierarchy, scene);
        // set main method
        world.setMainMethod(converter.convertMethod(scene.getMainMethod()));
        // set implicit entries
        world.setImplicitEntries(implicitEntries.stream()
                .map(hierarchy::getJREMethod)
                .collect(Collectors.toList()));
        return world;
    }

    private static void buildClasses(ClassHierarchy hierarchy, Scene scene) {
        scene.getClasses().forEach(c ->
                // TODO: dump classes
                hierarchy.getDefaultClassLoader().loadClass(c.getName()));
    }

    private static void initSoot(Scene scene) {
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
}
