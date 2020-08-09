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

import bamboo.callgraph.CallKind;
import bamboo.pta.core.ProgramManager;
import bamboo.pta.element.CallSite;
import bamboo.pta.element.Method;
import bamboo.pta.element.Obj;
import bamboo.pta.element.Type;
import bamboo.pta.element.Variable;
import bamboo.pta.env.EnvObj;
import bamboo.pta.statement.Allocation;
import bamboo.pta.statement.Call;

import java.util.Collections;

/**
 * Convenient methods for creating native models.
 */
class Utils {

    /**
     * Create allocation site and the corresponding constructor call site.
     * This method only supports non-argument constructor.
     * @param pm the program manager
     * @param container method containing the allocation site
     * @param type type of the allocated object
     * @param name name of the allocated object
     * @param recv variable holds the allocated object and
     *            acts as the receiver variable for the constructor call
     * @param ctorSig signature of the constructor
     * @param callId ID of the mock constructor call site
     */
    static void modelAllocation(
            ProgramManager pm, Method container,
            Type type, String name, Variable recv,
            String ctorSig, String callId) {
        Obj obj = new EnvObj(name, type, container);
        container.addStatement(new Allocation(recv, obj));
        Method ctor = pm.getUniqueMethodBySignature(ctorSig);
        MockCallSite initCallSite = new MockCallSite(
                CallKind.SPECIAL, ctor,
                recv, Collections.emptyList(),
                container, callId);
        Call initCall = new Call(initCallSite, null);
        container.addStatement(initCall);
    }

    /**
     * Model the side effects of a static native call r = T.foo(o, ...)
     * by mocking a virtual call r = o.m().
     */
    static void modelStaticToVirtualCall(
            ProgramManager pm, Method container, Call call,
            String calleeSig, String callId) {
        CallSite origin = call.getCallSite();
        origin.getArg(0).ifPresent(arg0 -> {
            Method callee = pm.getUniqueMethodBySignature(calleeSig);
            MockCallSite callSite = new MockCallSite(CallKind.VIRTUAL, callee,
                    arg0, Collections.emptyList(),
                    container, callId);
            Call mockCall = new Call(callSite, call.getLHS().orElse(null));
            container.addStatement(mockCall);
        });
    }
}
