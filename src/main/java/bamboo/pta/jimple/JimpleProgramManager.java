/*
 * Bamboo - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Bamboo is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package bamboo.pta.jimple;

import bamboo.pta.core.ProgramManager;
import bamboo.pta.element.CallSite;
import bamboo.pta.element.Field;
import bamboo.pta.element.Method;
import bamboo.pta.element.Type;
import bamboo.pta.env.Environment;
import bamboo.util.AnalysisException;
import soot.ArrayType;
import soot.FastHierarchy;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static soot.SootClass.HIERARCHY;

/**
 * Interface between soot and pointer analysis.
 */
public class JimpleProgramManager implements ProgramManager {

    private static final List<String> implicitEntries = Arrays.asList(
            "<java.lang.System: void initializeSystemClass()>",
            "<java.lang.Thread: void <init>(java.lang.ThreadGroup,java.lang.Runnable)>",
            "<java.lang.Thread: void <init>(java.lang.ThreadGroup,java.lang.String)>",
            "<java.lang.ThreadGroup: void <init>()>",
            "<java.lang.Thread: void exit()>",
            "<java.lang.ThreadGroup: void uncaughtException(java.lang.Thread,java.lang.Throwable)>",
            "<java.lang.ClassLoader: void <init>()>",
            "<java.lang.ClassLoader: java.lang.Class loadClassInternal(java.lang.String)>",
            "<java.lang.ClassLoader: void checkPackageAccess(java.lang.Class,java.security.ProtectionDomain)>",
            "<java.lang.ClassLoader: void addClass(java.lang.Class)>",
            "<java.lang.ClassLoader: long findNative(java.lang.ClassLoader,java.lang.String)>",
            "<java.security.PrivilegedActionException: void <init>(java.lang.Exception)>"
    );
    private final Scene scene;
    private final FastHierarchy hierarchy;
    private final Environment env = new Environment();
    private final IRBuilder irBuilder = new IRBuilder(env);

    public JimpleProgramManager(Scene scene) {
        this.scene = scene;
        this.hierarchy = scene.getOrMakeFastHierarchy();
        env.setup(this);
    }

    public static void initSoot(Scene scene) {
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

    @Override
    public Method getMainMethod() {
        return irBuilder.getMethod(scene.getMainMethod());
    }

    @Override
    public Collection<Method> getImplicitEntries() {
        return implicitEntries.stream()
                .filter(scene::containsMethod)
                .map(scene::getMethod)
                .map(irBuilder::getMethod)
                .collect(Collectors.toList());
    }

    @Override
    public Collection<Method> getAllMethods() {
        int nThreads = Runtime.getRuntime().availableProcessors();
        List<List<SootMethod>> groups = new ArrayList<>();
        for (int i = 0; i < nThreads; ++i) {
            groups.add(new ArrayList<>());
        }
        List<SootClass> classes = new ArrayList<>(scene.getClasses());
        int i = 0;
        for (SootClass c : classes) {
            for (SootMethod m : c.getMethods()) {
                if (!m.isConcrete() || m.isPhantom()) {
                    continue;
                } else if (m.isConcrete() || m.isNative()) {
                    groups.get(i++ % nThreads).add(m);
                }
            }
        }
        ExecutorService service = Executors.newFixedThreadPool(nThreads);
        for (List<SootMethod> group : groups) {
            service.execute(() -> group.forEach(irBuilder::getMethod));
        }
        service.shutdown();
        try {
            service.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return groups.stream()
                .flatMap(Collection::stream)
                .map(irBuilder::getMethod)
                .collect(Collectors.toList());
    }

    @Override
    public Environment getEnvironment() {
        return env;
    }

    @Override
    public boolean canAssign(Type from, Type to) {
        return hierarchy.canStoreType(
                ((JimpleType) from).getSootType(),
                ((JimpleType) to).getSootType());
    }

    @Override
    public boolean isSubtype(Type parent, Type child) {
        SootClass parentCls = ((JimpleType) parent).getSootClass();
        SootClass childCls = ((JimpleType) child).getSootClass();
        return parentCls != null && childCls != null
                && hierarchy.isSubclass(childCls, parentCls);
    }

    @Override
    public Method resolveInterfaceOrVirtualCall(Type recvType, Method method) {
        JimpleType jType = (JimpleType) recvType;
        JimpleMethod jMethod = (JimpleMethod) method;
        soot.Type type = jType.getSootType();
        soot.RefType concreteType;
        if (type instanceof ArrayType) {
            concreteType = RefType.v("java.lang.Object");
        } else if (type instanceof RefType) {
            concreteType = (RefType) type;
        } else {
            throw new AnalysisException("Unknown type: " + type);
        }
        SootMethod callee = hierarchy.resolveConcreteDispatch(
                concreteType.getSootClass(),
                jMethod.getSootMethod());
        return irBuilder.getMethod(callee);
    }

    @Override
    public Method resolveSpecialCall(CallSite callSite, Method container) {
        SootMethod target = ((JimpleMethod) callSite.getMethod())
                .getSootMethod();
        SootMethod jContainer = ((JimpleMethod) container).getSootMethod();
        SootMethod callee;
        // This implementation is based on FastHierarchy.resolveSpecialDispatch()
        if (target.getName().equals("<init>") || target.isPrivate()) {
            callee = target;
        } else if (hierarchy.isSubclass(target.getDeclaringClass(),
                jContainer.getDeclaringClass())) {
            callee = hierarchy.resolveConcreteDispatch(
                    jContainer.getDeclaringClass(), target);
        } else {
            callee = target;
        }
        return irBuilder.getMethod(callee);
    }

    IRBuilder getIRBuilder() {
        return irBuilder;
    }

    @Override
    public Type getUniqueTypeByName(String typeName) {
        return irBuilder.getType(scene.getType(typeName));
    }

    @Override
    public Optional<Type> tryGetUniqueTypeByName(String typeName) {
        soot.Type sootType = scene.getTypeUnsafe(typeName);
        return sootType != null ?
                Optional.of(irBuilder.getType(sootType)) :
                Optional.empty();
    }

    @Override
    public Field getUniqueFieldBySignature(String fieldSig) {
        return irBuilder.getField(scene.getField(fieldSig));
    }

    @Override
    public Method getUniqueMethodBySignature(String methodSig) {
        return irBuilder.getMethod(scene.getMethod(methodSig));
    }
}
