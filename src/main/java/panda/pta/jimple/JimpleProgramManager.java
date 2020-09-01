/*
 * Panda - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Panda is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package panda.pta.jimple;

import panda.pta.core.ProgramManager;
import panda.pta.element.CallSite;
import panda.pta.element.Field;
import panda.pta.element.Method;
import panda.pta.element.Obj;
import panda.pta.element.Type;
import panda.pta.env.Environment;
import panda.util.AnalysisException;
import soot.ArrayType;
import soot.FastHierarchy;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.util.NumberedString;

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
    private final NumberedString CLINIT_SIG;
    private final Environment env = new Environment();
    private final IRBuilder irBuilder = new IRBuilder(env);

    public JimpleProgramManager(Scene scene) {
        this.scene = scene;
        hierarchy = scene.getOrMakeFastHierarchy();
        CLINIT_SIG = scene.getSubSigNumberer().findOrAdd("void <clinit>()");
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
    public void buildIRForAllMethods() {
        irBuilder.buildAllMethods(scene);
    }

    @Override
    public Environment getEnvironment() {
        return env;
    }

    IRBuilder getIRBuilder() {
        return irBuilder;
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
    public Method resolveCallee(Obj recvObj, CallSite callSite) {
        switch (callSite.getKind()) {
            case INTERFACE:
            case VIRTUAL:
                return resolveInterfaceOrVirtualCall(
                        recvObj.getType(), callSite);
            case SPECIAL:
                return resolveSpecialCall(
                        callSite, callSite.getContainerMethod());
            case STATIC:
                return resolveStaticCall(callSite);
            default:
                throw new AnalysisException("Unknown CallSite: " + callSite);
        }
    }

    /**
     * @param recvType type of receiver object
     * @param callSite the call site
     * @return the callee
     */
    private Method resolveInterfaceOrVirtualCall(
            Type recvType, CallSite callSite) {
        return dispatch(recvType, callSite.getMethod());
    }

    /**
     * @param callSite the call site
     * @param container containing method of the call site
     * @return the callee
     */
    private Method resolveSpecialCall(CallSite callSite, Method container) {
        SootMethod target = ((JimpleMethod) callSite.getMethod())
                .getSootMethod();
        SootMethod sootContainer = ((JimpleMethod) container).getSootMethod();
        SootMethod callee;
        // This implementation is based on FastHierarchy.resolveSpecialDispatch()
        if (target.getName().equals("<init>") || target.isPrivate()) {
            callee = target;
        } else if (hierarchy.isSubclass(target.getDeclaringClass(),
                sootContainer.getDeclaringClass())) {
            callee = hierarchy.resolveConcreteDispatch(
                    sootContainer.getDeclaringClass(), target);
        } else {
            callee = target;
        }
        return irBuilder.getMethod(callee);
    }

    /**
     * @param callSite the call site
     * @return the callee
     */
    private Method resolveStaticCall(CallSite callSite) {
        JimpleMethod target = (JimpleMethod) callSite.getMethod();
        SootMethod callee = target.getSootMethod();
        return irBuilder.getMethod(callee);
    }

    @Override
    public Method dispatch(Type recvType, Method target) {
        soot.Type sootType = ((JimpleType) recvType).getSootType();
        SootMethod sootMethod = ((JimpleMethod) target).getSootMethod();
        soot.RefType concreteType;
        if (sootType instanceof ArrayType) {
            concreteType = RefType.v("java.lang.Object");
        } else if (sootType instanceof RefType) {
            concreteType = (RefType) sootType;
        } else {
            throw new AnalysisException("Unknown type: " + sootType);
        }
        SootMethod callee = hierarchy.resolveConcreteDispatch(
                concreteType.getSootClass(), sootMethod);
        return irBuilder.getMethod(callee);
    }

    @Override
    public Optional<Method> getClassInitializerOf(Type type) {
        if (type.isClassType()) {
            SootClass cls = ((JimpleType) type).getSootClass();
            SootMethod clinit = cls.getMethodUnsafe(CLINIT_SIG);
            if (clinit != null) {
                return Optional.of(irBuilder.getMethod(clinit));
            }
        }
        return Optional.empty();
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
