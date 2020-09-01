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

package panda.pta.env.nativemodel;

import panda.pta.core.ProgramManager;
import panda.pta.element.CallSite;
import panda.pta.element.Method;
import panda.pta.element.Type;
import panda.pta.element.Variable;
import panda.pta.options.Options;
import panda.pta.statement.ArrayLoad;
import panda.pta.statement.ArrayStore;
import panda.pta.statement.AssignCast;
import panda.pta.statement.Call;
import panda.pta.statement.StatementVisitor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

class CallModel implements StatementVisitor {

    private final ProgramManager pm;
    // Use String as key is to avoid cyclic dependence during the
    // initialization of ProgramManager.
    // TODO: use Method as key to improve performance?
    private final Map<String, BiConsumer<Method, Call>> handlers;
    /**
     * Counter to give each mock variable an unique name in each method.
     */
    private final ConcurrentMap<Method, AtomicInteger> counter;

    CallModel(ProgramManager pm) {
        this.pm = pm;
        handlers = new HashMap<>();
        counter = new ConcurrentHashMap<>();
        initHandlers();
    }

    @Override
    public void visit(Call call) {
        CallSite callSite = call.getCallSite();
        Method callee = callSite.getMethod();
        BiConsumer<Method, Call> handler =
                handlers.get(callee.getSignature());
        if (handler != null) {
            Method container = callSite.getContainerMethod();
            handler.accept(container, call);
        }
    }

    private void initHandlers() {
        // --------------------------------------------------------------------
        // java.lang.System
        // --------------------------------------------------------------------
        // <java.lang.System: void arraycopy(java.lang.Object,int,java.lang.Object,int,int)>
        registerHandler("<java.lang.System: void arraycopy(java.lang.Object,int,java.lang.Object,int,int)>", (method, call) -> {
            Optional<Variable> src = call.getCallSite().getArg(0);
            Optional<Variable> dest = call.getCallSite().getArg(2);
            if (src.isPresent() && dest.isPresent()) {
                Type arrayType = pm.getUniqueTypeByName("java.lang.Object[]");
                Variable srcArray = newMockVariable(arrayType , method);
                Variable destArray = newMockVariable(arrayType , method);
                Variable temp = newMockVariable(
                        pm.getUniqueTypeByName("java.lang.Object"), method);
                // src/dest may point to non-array objects due to imprecision
                // of pointer analysis, thus we add cast statements to filter
                // out load/store operations on non-array objects.
                // Note that the cast statements will exclude primitive arrays.
                method.addStatement(new AssignCast(srcArray, arrayType, src.get()));
                method.addStatement(new AssignCast(destArray, arrayType, dest.get()));
                method.addStatement(new ArrayLoad(temp, srcArray));
                method.addStatement(new ArrayStore(destArray, temp));
            }
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
        if (Options.get().jdkVersion() <= 6) {
            registerHandler("<java.lang.ref.Finalizer: void invokeFinalizeMethod(java.lang.Object)>", (method, call) -> {
                Utils.modelStaticToVirtualCall(pm, method, call,
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
        //  in a PriviligedActionException.
        // <java.security.AccessController: java.lang.Object doPrivileged(java.security.PrivilegedAction)>
        registerHandler("<java.security.AccessController: java.lang.Object doPrivileged(java.security.PrivilegedAction)>", (method, call) -> {
            Utils.modelStaticToVirtualCall(pm, method, call,
                    "<java.security.PrivilegedAction: java.lang.Object run()>",
                    "doPrivileged");
        });
        // <java.security.AccessController: java.lang.Object doPrivileged(java.security.PrivilegedAction,java.security.AccessControlContext)>
        registerHandler("<java.security.AccessController: java.lang.Object doPrivileged(java.security.PrivilegedAction,java.security.AccessControlContext)>", (method, call) -> {
            Utils.modelStaticToVirtualCall(pm, method, call,
                    "<java.security.PrivilegedAction: java.lang.Object run()>",
                    "doPrivileged");
        });
        // <java.security.AccessController: java.lang.Object doPrivileged(java.security.PrivilegedExceptionAction)>
        registerHandler("<java.security.AccessController: java.lang.Object doPrivileged(java.security.PrivilegedExceptionAction)>", (method, call) -> {
            Utils.modelStaticToVirtualCall(pm, method, call,
                    "<java.security.PrivilegedExceptionAction: java.lang.Object run()>",
                    "doPrivileged");
        });
        // <java.security.AccessController: java.lang.Object doPrivileged(java.security.PrivilegedExceptionAction,java.security.AccessControlContext)>
        registerHandler("<java.security.AccessController: java.lang.Object doPrivileged(java.security.PrivilegedExceptionAction,java.security.AccessControlContext)>", (method, call) -> {
            Utils.modelStaticToVirtualCall(pm, method, call,
                    "<java.security.PrivilegedExceptionAction: java.lang.Object run()>",
                    "doPrivileged");
        });
    }

    private void registerHandler(String signature,
                                 BiConsumer<Method, Call> handler) {
        handlers.put(signature, handler);
    }

    private Variable newMockVariable(Type type, Method container) {
        int id = counter.computeIfAbsent(container,
                (k) -> new AtomicInteger(0))
                .getAndIncrement();
        return new MockVariable(type, container, "@native-call-mock-var" + id);
    }
}
