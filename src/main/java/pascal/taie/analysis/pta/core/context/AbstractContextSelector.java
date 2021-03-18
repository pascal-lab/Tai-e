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

package pascal.taie.analysis.pta.core.context;

import pascal.taie.analysis.pta.core.cs.CSMethod;
import pascal.taie.analysis.pta.core.heap.NewObj;
import pascal.taie.analysis.pta.core.heap.Obj;

abstract class AbstractContextSelector<T> implements ContextSelector {

    protected final ContextFactory<T> factory = new TreeContext.Factory<>();

    @Override
    public Context getDefaultContext() {
        return factory.getDefaultContext();
    }

    @Override
    public Context selectHeapContext(CSMethod method, Obj obj) {
        // Uses different strategies to select heap contexts
        // for different kinds of objects.
        if (obj instanceof NewObj) {
            return doSelectHeapContext(method, obj);
        } else {
            return getDefaultContext();
        }
    }

    /**
     * This method defines the real heap context selector.
     */
    protected abstract Context doSelectHeapContext(CSMethod method, Obj obj);
}
