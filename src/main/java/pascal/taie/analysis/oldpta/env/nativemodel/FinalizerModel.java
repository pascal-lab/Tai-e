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

import pascal.taie.analysis.graph.callgraph.CallKind;
import pascal.taie.analysis.oldpta.ir.Allocation;
import pascal.taie.analysis.oldpta.ir.Call;
import pascal.taie.analysis.oldpta.ir.Obj;
import pascal.taie.analysis.oldpta.ir.PTAIR;
import pascal.taie.analysis.oldpta.ir.Statement;
import pascal.taie.analysis.oldpta.ir.Variable;
import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.types.Type;

import java.util.Collections;

/**
 * Call Finalizer.register() at allocation sites of objects which override
 * Object.finalize() method.
 * NOTE: finalize() has been deprecated starting with Java 9, and will
 * eventually be removed.
 */
class FinalizerModel {

    private final ClassHierarchy hierarchy;

    private final JMethod finalize;

    private final MethodRef finalizeRef;

    private final MethodRef registerRef;

    FinalizerModel(ClassHierarchy hierarchy) {
        this.hierarchy = hierarchy;
        finalize = hierarchy.getJREMethod("<java.lang.Object: void finalize()>");
        finalizeRef = finalize.getRef();
        registerRef = hierarchy.getJREMethod("<java.lang.ref.Finalizer: void register(java.lang.Object)>")
                .getRef();
    }

    void process(Statement s, PTAIR containerIR) {
        if (s instanceof Allocation) {
            Allocation alloc = (Allocation) s;
            Obj obj = alloc.getObject();
            if (hasOverriddenFinalize(obj.getType())) {
                obj.getContainerMethod().ifPresent(container -> {
                    Variable lhs = alloc.getVar();
                    MockCallSite callSite = new MockCallSite(CallKind.STATIC,
                            registerRef, null, Collections.singletonList(lhs),
                            container, "register-finalize");
                    Call call = new Call(callSite, null);
                    containerIR.addStatement(call);
                });
            }
        }
    }

    private boolean hasOverriddenFinalize(Type type) {
        return !hierarchy.dispatch(type, finalizeRef).equals(finalize);
    }
}
