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

package pascal.taie.analysis.pta.core.cs.selector;

import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.context.ListContext;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.language.classes.JMethod;

public class _2ObjSelector implements ContextSelector {

    @Override
    public Context getDefaultContext() {
        return ListContext.make();
    }

    @Override
    public Context selectContext(CSCallSite callSite, JMethod callee) {
        return callSite.getContext();
    }

    @Override
    public Context selectContext(CSCallSite callSite, CSObj recv, JMethod callee) {
        Context hctx = recv.getContext();
        Obj obj = recv.getObject();
        return hctx.getLength() == 0 ?
                ListContext.make(obj) :
                ListContext.make(hctx.getElementAt(hctx.getLength() - 1), obj);
    }

    @Override
    public Context selectHeapContext(CSMethod method, Obj obj) {
        Context ctx = method.getContext();
        return ctx.getLength() <= 1 ? ctx :
                ListContext.make(ctx.getElementAt(ctx.getLength() - 1));
    }
}
