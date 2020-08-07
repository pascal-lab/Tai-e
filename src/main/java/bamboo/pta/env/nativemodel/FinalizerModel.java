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
import bamboo.pta.element.Method;
import bamboo.pta.element.Obj;
import bamboo.pta.element.Type;
import bamboo.pta.element.Variable;
import bamboo.pta.statement.Allocation;
import bamboo.pta.statement.Call;
import bamboo.pta.statement.StatementVisitor;

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
        return !pm.resolveInterfaceOrVirtualCall(type, finalize)
                .equals(finalize);
    }
}
