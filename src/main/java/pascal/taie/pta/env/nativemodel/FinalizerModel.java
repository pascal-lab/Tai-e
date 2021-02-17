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
import pascal.taie.pta.ir.Allocation;
import pascal.taie.pta.ir.Call;
import pascal.taie.pta.ir.Obj;
import pascal.taie.pta.ir.StatementVisitor;
import pascal.taie.pta.ir.Variable;

import java.util.Collections;

/**
 * Call Finalizer.register() at allocation sites of objects which override
 * Object.finalize() method.
 * NOTE: finalize() has been deprecated starting with Java 9, and will
 * eventually be removed.
 */
class FinalizerModel implements StatementVisitor {

    private final ClassHierarchy hierarchy;

    private final JMethod finalize;

    private final MethodReference finalizeRef;

    private final MethodReference registerRef;

    FinalizerModel(ClassHierarchy hierarchy) {
        this.hierarchy = hierarchy;
        finalize = hierarchy.getJREMethod("<java.lang.Object: void finalize()>");
        finalizeRef = finalize.getRef();
        registerRef = hierarchy.getJREMethod("<java.lang.ref.Finalizer: void register(java.lang.Object)>")
                .getRef();
    }

    @Override
    public void visit(Allocation alloc) {
        Obj obj = alloc.getObject();
        if (isOverridesFinalize(obj.getType())) {
            obj.getContainerMethod().ifPresent(container -> {
                Variable lhs = alloc.getVar();
                MockCallSite callSite = new MockCallSite(CallKind.STATIC,
                        registerRef, null, Collections.singletonList(lhs),
                        container, "register-finalize");
                Call call = new Call(callSite, null);
                container.getIR().addStatement(call);
            });
        }
    }

    private boolean isOverridesFinalize(Type type) {
        return hierarchy.dispatch(type, finalizeRef).equals(finalize);
    }
}
