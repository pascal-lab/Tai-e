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
import soot.SootMethod;
import soot.SourceLocator;
import soot.jimple.SpecialInvokeExpr;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
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
    private final Environment env = new Environment(this);
    private final IRBuilder irBuilder = new IRBuilder(env);

    public JimpleProgramManager(Scene scene) {
        this.scene = scene;
        this.hierarchy = scene.getOrMakeFastHierarchy();
        addBasicClasses(scene);
    }

    public static void initSoot(Scene scene) {
        // The following line is necessary to avoid a runtime exception
        // when running soot with java 1.8
        scene.addBasicClass("sun.util.locale.provider.HostLocaleProviderAdapterImpl", HIERARCHY);
    }

    private void addBasicClasses(Scene scene) {
        /*
         * For simulating the FileSystem class, we need the implementation
         * of the FileSystem, but the classes are not loaded automatically
         * due to the indirection via native code.
         */
        addCommonDynamicClass(scene, "java.io.UnixFileSystem");
        addCommonDynamicClass(scene, "java.io.WinNTFileSystem");
        addCommonDynamicClass(scene, "java.io.Win32FileSystem");

        /* java.net.URL loads handlers dynamically */
        addCommonDynamicClass(scene, "sun.net.www.protocol.file.Handler");
        addCommonDynamicClass(scene, "sun.net.www.protocol.ftp.Handler");
        addCommonDynamicClass(scene, "sun.net.www.protocol.http.Handler");
        addCommonDynamicClass(scene, "sun.net.www.protocol.https.Handler");
        addCommonDynamicClass(scene, "sun.net.www.protocol.jar.Handler");
    }

    private void addCommonDynamicClass(Scene scene, String className) {
        // NOTE: SourceLocator.getClassSource() has side effect of
        // modifying its classPath, so it MUST be called AFTER
        // Soot has initialized the class path.
        if (SourceLocator.v().getClassSource(className) != null) {
            scene.addBasicClass(className);
        }
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
    public boolean canAssign(Type from, Type to) {
        return hierarchy.canStoreType(
                ((JimpleType) from).getSootType(),
                ((JimpleType) to).getSootType());
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
        JimpleCallSite jCallSite = (JimpleCallSite) callSite;
        JimpleMethod jContainer = (JimpleMethod) container;
        SootMethod callee = hierarchy.resolveSpecialDispatch(
                (SpecialInvokeExpr) jCallSite.getSootInvokeExpr(),
                jContainer.getSootMethod());
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
