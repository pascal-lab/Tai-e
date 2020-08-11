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

package bamboo.pta.env.nativemodel;

import bamboo.pta.core.ProgramManager;
import bamboo.pta.element.CallSite;
import bamboo.pta.element.Method;
import bamboo.pta.element.Type;
import bamboo.pta.element.Variable;
import bamboo.pta.options.Options;
import bamboo.pta.statement.ArrayLoad;
import bamboo.pta.statement.ArrayStore;
import bamboo.pta.statement.Call;
import bamboo.pta.statement.StatementVisitor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

class CallModel implements StatementVisitor {

    private final ProgramManager pm;
    // Use String as key is to avoid cyclic dependence during the
    // initialization of ProgramManager.
    // TODO: use Method as key to improve performance?
    private final Map<String, BiConsumer<Method, Call>> handlers;
    /**
     * Counter to give each mock variable an unique name.
     */
    private final AtomicInteger counter;

    CallModel(ProgramManager pm) {
        this.pm = pm;
        handlers = new HashMap<>();
        counter = new AtomicInteger(0);
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
        /**********************************************************************
         * java.lang.System
         *********************************************************************/
        // <java.lang.System: void arraycopy(java.lang.Object,int,java.lang.Object,int,int)>
        registerHandler("<java.lang.System: void arraycopy(java.lang.Object,int,java.lang.Object,int,int)>", (method, call) -> {
            Variable temp = newMockVariable(
                    pm.getUniqueTypeByName("java.lang.Object"), method);
            Optional<Variable> src = call.getCallSite().getArg(0);
            Optional<Variable> dest = call.getCallSite().getArg(2);
            if (src.isPresent() && dest.isPresent()) {
                method.addStatement(new ArrayLoad(temp, src.get()));
                method.addStatement(new ArrayStore(dest.get(), temp));
            }
        });

        /**********************************************************************
         * java.lang.ref.Finalizer
         *********************************************************************/
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

        /**********************************************************************
         * java.security.AccessController
         *********************************************************************/
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
        return new MockVariable(type, container,
                "@native-call-mock-var" + counter.getAndIncrement());
    }
}
