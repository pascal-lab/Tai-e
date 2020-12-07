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

package pascal.taie.pta.env.nativemodel;

import pascal.taie.callgraph.CallKind;
import pascal.taie.pta.core.ProgramManager;
import pascal.taie.pta.element.Method;
import pascal.taie.pta.element.Obj;
import pascal.taie.pta.element.Type;
import pascal.taie.pta.element.Variable;
import pascal.taie.pta.statement.Allocation;
import pascal.taie.pta.statement.Call;
import pascal.taie.pta.statement.StatementVisitor;

import java.util.Collections;

/**
 * Call Finalizer.register() at allocation sites of objects which override
 * Object.finalize() method.
 * NOTE: finalize() has been deprecated starting with Java 9, and will
 * eventually be removed.
 */
class FinalizerModel implements StatementVisitor {

    private final ProgramManager pm;
    private Method finalize;
    private Method register;

    FinalizerModel(ProgramManager pm) {
        this.pm = pm;
    }

    @Override
    public void visit(Allocation alloc) {
        Obj obj = alloc.getObject();
        if (isOverridesFinalize(obj.getType())) {
            obj.getContainerMethod().ifPresent(container -> {
                Variable lhs = alloc.getVar();
                MockCallSite callSite = new MockCallSite(CallKind.STATIC,
                        getRegister(), null, Collections.singletonList(lhs),
                        container, "register-finalize");
                Call call = new Call(callSite, null);
                container.addStatement(call);
            });
        }
    }

    private Method getFinalize() {
        if (finalize == null) {
            finalize = pm.getUniqueMethodBySignature(
                    "<java.lang.Object: void finalize()>");
        }
        return finalize;
    }

    private Method getRegister() {
        if (register == null) {
            register = pm.getUniqueMethodBySignature(
                    "<java.lang.ref.Finalizer: void register(java.lang.Object)>");
        }
        return register;
    }

    private boolean isOverridesFinalize(Type type) {
        Method finalize = getFinalize();
        return !pm.dispatch(type, finalize).equals(finalize);
    }
}
