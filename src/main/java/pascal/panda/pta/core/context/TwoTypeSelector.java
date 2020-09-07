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

package pascal.panda.pta.core.context;

import pascal.panda.pta.core.cs.CSCallSite;
import pascal.panda.pta.core.cs.CSMethod;
import pascal.panda.pta.core.cs.CSObj;
import pascal.panda.pta.element.Method;
import pascal.panda.pta.element.Obj;
import pascal.panda.pta.element.Type;

public class TwoTypeSelector extends AbstractContextSelector {

    @Override
    public Context selectContext(CSCallSite callSite, Method callee) {
        return callSite.getContext();
    }

    @Override
    public Context selectContext(CSCallSite callSite, CSObj recv, Method callee) {
        Context hctx = recv.getContext();
        Type type = recv.getObject().getContainerType();
        return hctx.depth() == 0 ?
                new OneContext<>(type) :
                new TwoContext<>(hctx.element(hctx.depth()), type);
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
