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
import pascal.taie.analysis.pta.core.cs.context.ContextFactory;
import pascal.taie.analysis.pta.core.cs.context.TreeContext;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.heap.NewObj;
import pascal.taie.analysis.pta.core.heap.Obj;

abstract class AbstractContextSelector<T> implements ContextSelector {

    protected final ContextFactory<T> factory = new TreeContext.Factory<>();

    @Override
    public Context getEmptyContext() {
        return factory.getEmptyContext();
    }

    @Override
    public Context selectHeapContext(CSMethod method, Obj obj) {
        // Uses different strategies to select heap contexts
        // for different kinds of objects.
        if (obj instanceof NewObj) {
            return selectNewObjContext(method, (NewObj) obj);
        } else {
            return getEmptyContext();
        }
    }

    /**
     * Defines the real heap context selector for NewObj.
     */
    protected abstract Context selectNewObjContext(CSMethod method, NewObj obj);
}
