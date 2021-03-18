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

package pascal.taie.analysis.oldpta.env.nativemodel;

import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.StringReps;
import pascal.taie.language.types.Type;
import pascal.taie.language.types.TypeManager;
import pascal.taie.analysis.oldpta.ir.ArrayLoad;
import pascal.taie.analysis.oldpta.ir.ArrayStore;
import pascal.taie.analysis.oldpta.ir.AssignCast;
import pascal.taie.analysis.oldpta.ir.Call;
import pascal.taie.analysis.oldpta.ir.CallSite;
import pascal.taie.analysis.oldpta.ir.PTAIR;
import pascal.taie.analysis.oldpta.ir.Statement;
import pascal.taie.analysis.oldpta.ir.Variable;
import pascal.taie.analysis.pta.PTAOptions;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import static pascal.taie.util.collection.CollectionUtils.newConcurrentMap;
import static pascal.taie.util.collection.CollectionUtils.newMap;

class CallModel {

    private final ClassHierarchy hierarchy;

    private final TypeManager typeManager;

    // Use String as key is to avoid cyclic dependence during the
    // initialization of ProgramManager.
    // TODO: use Method as key to improve performance?
    private final Map<String, BiConsumer<PTAIR, Call>> handlers;

    /**
     * Counter to give each mock variable an unique name in each method.
     */
    private final ConcurrentMap<JMethod, AtomicInteger> counter;

    CallModel(ClassHierarchy hierarchy, TypeManager typeManager) {
        this.hierarchy = hierarchy;
        this.typeManager = typeManager;
        handlers = newMap();
        counter = newConcurrentMap();
        initHandlers();
    }

    void process(Statement s, PTAIR containerIR) {
        if (s instanceof Call) {
            Call call = (Call) s;
            CallSite callSite = call.getCallSite();
            JMethod callee = callSite.getMethodRef().resolve();
            BiConsumer<PTAIR, Call> handler =
                    handlers.get(callee.getSignature());
            if (handler != null) {
                handler.accept(containerIR, call);
            }
        }
    }

    private void initHandlers() {
        // --------------------------------------------------------------------
        // java.lang.System
        // --------------------------------------------------------------------
        // <java.lang.System: void arraycopy(java.lang.Object,int,java.lang.Object,int,int)>
        registerHandler("<java.lang.System: void arraycopy(java.lang.Object,int,java.lang.Object,int,int)>", (ir, call) -> {
            JMethod method = ir.getMethod();
            Variable src = call.getCallSite().getArg(0);
            Variable dest = call.getCallSite().getArg(2);
            Type objType = typeManager.getClassType(StringReps.OBJECT);
            Type arrayType = typeManager.getArrayType(objType, 1);
            Variable srcArray = newMockVariable(arrayType , method);
            Variable destArray = newMockVariable(arrayType , method);
            Variable temp = newMockVariable(objType, method);
            // src/dest may point to non-array objects due to imprecision
            // of pointer analysis, thus we add cast statements to filter
            // out load/store operations on non-array objects.
            // Note that the cast statements will exclude primitive arrays.
            ir.addStatement(new AssignCast(srcArray, arrayType, src));
            ir.addStatement(new AssignCast(destArray, arrayType, dest));
            ir.addStatement(new ArrayLoad(temp, srcArray));
            ir.addStatement(new ArrayStore(destArray, temp));
        });

        // --------------------------------------------------------------------
        // java.lang.ref.Finalizer
        // --------------------------------------------------------------------
        // <java.lang.ref.Finalizer: void invokeFinalizeMethod(java.lang.Object)>
        //
        // Indirect invocations of finalize methods from java.lang.ref.Finalizer.
        // Object.finalize is a protected method, so it cannot be directly
        // invoked. Finalizer uses an indirection via native code to
        // circumvent this. This rule implements this indirection.
        // This API is deprecated since Java 7.
        if (PTAOptions.get().jdkVersion() <= 6) {
            registerHandler("<java.lang.ref.Finalizer: void invokeFinalizeMethod(java.lang.Object)>", (ir, call) -> {
                Utils.modelStaticToVirtualCall(hierarchy, ir, call,
                        "<java.lang.Object: void finalize()>",
                        "invoke-finalize");
            });
        }

        // --------------------------------------------------------------------
        // java.security.AccessController
        // --------------------------------------------------------------------
        // The run methods of privileged actions are invoked through the
        // AccessController.doPrivileged method. This introduces an
        // indirection via native code that needs to be simulated in a pointer
        // analysis.
        //
        // Call from an invocation of doPrivileged to an implementation of the
        // PrivilegedAction.run method that will be indirectly invoked.
        //
        // The first parameter of a doPrivileged invocation (a
        // PrivilegedAction) is assigned to the 'this' variable of 'run()'
        // method invocation.
        //
        // The return variable of the 'run()' method of a privileged action is
        // assigned to the return result of the doPrivileged method
        // invocation.
        //
        // TODO for PrivilegedExceptionAction, catch exceptions and wrap them
        //  in a PrivilegedActionException.
        // <java.security.AccessController: java.lang.Object doPrivileged(java.security.PrivilegedAction)>
        registerHandler("<java.security.AccessController: java.lang.Object doPrivileged(java.security.PrivilegedAction)>", (ir, call) -> {
            Utils.modelStaticToVirtualCall(hierarchy, ir, call,
                    "<java.security.PrivilegedAction: java.lang.Object run()>",
                    "doPrivileged");
        });
        // <java.security.AccessController: java.lang.Object doPrivileged(java.security.PrivilegedAction,java.security.AccessControlContext)>
        registerHandler("<java.security.AccessController: java.lang.Object doPrivileged(java.security.PrivilegedAction,java.security.AccessControlContext)>", (ir, call) -> {
            Utils.modelStaticToVirtualCall(hierarchy, ir, call,
                    "<java.security.PrivilegedAction: java.lang.Object run()>",
                    "doPrivileged");
        });
        // <java.security.AccessController: java.lang.Object doPrivileged(java.security.PrivilegedExceptionAction)>
        registerHandler("<java.security.AccessController: java.lang.Object doPrivileged(java.security.PrivilegedExceptionAction)>", (ir, call) -> {
            Utils.modelStaticToVirtualCall(hierarchy, ir, call,
                    "<java.security.PrivilegedExceptionAction: java.lang.Object run()>",
                    "doPrivileged");
        });
        // <java.security.AccessController: java.lang.Object doPrivileged(java.security.PrivilegedExceptionAction,java.security.AccessControlContext)>
        registerHandler("<java.security.AccessController: java.lang.Object doPrivileged(java.security.PrivilegedExceptionAction,java.security.AccessControlContext)>", (ir, call) -> {
            Utils.modelStaticToVirtualCall(hierarchy, ir, call,
                    "<java.security.PrivilegedExceptionAction: java.lang.Object run()>",
                    "doPrivileged");
        });
    }

    private void registerHandler(String signature,
                                 BiConsumer<PTAIR, Call> handler) {
        handlers.put(signature, handler);
    }

    private Variable newMockVariable(Type type, JMethod container) {
        int id = counter.computeIfAbsent(container,
                (k) -> new AtomicInteger(0))
                .getAndIncrement();
        return new MockVariable(type, container, "@native-call-mock-var" + id);
    }
}
