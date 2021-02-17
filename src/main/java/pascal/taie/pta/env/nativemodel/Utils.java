/*
 * Tai-e: A Program Analysis Framework for Java
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

package pascal.taie.pta.env.nativemodel;

import pascal.taie.callgraph.CallKind;
import pascal.taie.java.ClassHierarchy;
import pascal.taie.java.classes.JMethod;
import pascal.taie.java.classes.MethodReference;
import pascal.taie.java.types.Type;
import pascal.taie.pta.core.ProgramManager;
import pascal.taie.pta.env.EnvObj;
import pascal.taie.pta.ir.Allocation;
import pascal.taie.pta.ir.Call;
import pascal.taie.pta.ir.CallSite;
import pascal.taie.pta.ir.Obj;
import pascal.taie.pta.ir.Variable;

import java.util.Collections;

/**
 * Convenient methods for creating native models.
 */
class Utils {

    /**
     * Create allocation site and the corresponding constructor call site.
     * This method only supports non-argument constructor.
     * @param hierarchy the class hierarchy
     * @param container method containing the allocation site
     * @param type type of the allocated object
     * @param name name of the allocated object
     * @param recv variable holds the allocated object and
     *            acts as the receiver variable for the constructor call
     * @param ctorSig signature of the constructor
     * @param callId ID of the mock constructor call site
     */
    static void modelAllocation(
            ClassHierarchy hierarchy, JMethod container,
            Type type, String name, Variable recv,
            String ctorSig, String callId) {
        Obj obj = new EnvObj(name, type, container);
        container.getIR().addStatement(new Allocation(recv, obj));
        JMethod ctor = hierarchy.getJREMethod(ctorSig);
        MethodReference ctorRef = MethodReference.get(ctor);
        MockCallSite initCallSite = new MockCallSite(
                CallKind.SPECIAL, ctorRef,
                recv, Collections.emptyList(),
                container, callId);
        Call initCall = new Call(initCallSite, null);
        container.getIR().addStatement(initCall);
    }

    /**
     * Model the side effects of a static native call r = T.foo(o, ...)
     * by mocking a virtual call r = o.m().
     */
    static void modelStaticToVirtualCall(
            ProgramManager pm, JMethod container, Call call,
            String calleeSig, String callId) {
        CallSite origin = call.getCallSite();
        origin.getArg(0).ifPresent(arg0 -> {
            JMethod callee = pm.getUniqueMethodBySignature(calleeSig);
            MockCallSite callSite = new MockCallSite(CallKind.VIRTUAL, callee,
                    arg0, Collections.emptyList(),
                    container, callId);
            Call mockCall = new Call(callSite, call.getLHS().orElse(null));
            container.addStatement(mockCall);
        });
    }
}
