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

package pascal.taie.analysis.oldpta.core.context;

import pascal.taie.analysis.oldpta.core.cs.CSCallSite;
import pascal.taie.analysis.oldpta.core.cs.CSMethod;
import pascal.taie.analysis.oldpta.core.cs.CSObj;
import pascal.taie.analysis.oldpta.ir.Obj;
import pascal.taie.language.classes.JMethod;

/**
 * 2-call-site-sensitivity with 1 heap context.
 */
public class TwoCallSelector extends AbstractContextSelector {

    @Override
    public Context selectContext(CSCallSite callSite, JMethod callee) {
        return selectContext(callSite, null, callee);
    }

    @Override
    public Context selectContext(CSCallSite callSite, CSObj recv, JMethod callee) {
        Context ctx = callSite.getContext();
        switch (ctx.depth()) {
            case 0:
                return new OneContext<>(callSite.getCallSite());
            case 1:
                return new TwoContext<>(
                        ctx.element(1), callSite.getCallSite());
            default:
                return new TwoContext<>(
                        ctx.element(ctx.depth()), callSite.getCallSite());
        }
    }

    @Override
    protected Context doSelectHeapContext(CSMethod method, Obj obj) {
        Context ctx = method.getContext();
        if (ctx.depth() < 2) {
            return ctx;
        } else {
            return new OneContext<>(ctx.element(ctx.depth()));
        }
    }
}
