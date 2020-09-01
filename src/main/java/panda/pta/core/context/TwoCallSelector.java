/*
 * Bamboo - A Program Analysis Framework for Java
 *
 * Copyright (C)  2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C)  2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Bamboo is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package panda.pta.core.context;

import panda.pta.core.cs.CSCallSite;
import panda.pta.core.cs.CSMethod;
import panda.pta.core.cs.CSObj;
import panda.pta.element.Method;
import panda.pta.element.Obj;

/**
 * 2-call-site-sensitivity with 1 heap context.
 */
public class TwoCallSelector extends AbstractContextSelector {

    @Override
    public Context selectContext(CSCallSite callSite, Method callee) {
        return selectContext(callSite, null, callee);
    }

    @Override
    public Context selectContext(CSCallSite callSite, CSObj recv, Method callee) {
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
