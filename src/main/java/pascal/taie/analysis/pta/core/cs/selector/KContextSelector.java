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
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.heap.NewObj;

/**
 * K-limiting context selector.
 * @param <T> type of context elements.
 */
abstract class KContextSelector<T> extends AbstractContextSelector<T> {

    /**
     * Limit of context length.
     */
    protected final int limit;

    /**
     * Limit of heap context length.
     */
    protected final int hlimit;

    /**
     *
     * @param k k-limit for method contexts.
     * @param hk k-limit for heap contexts.
     */
    KContextSelector(int k, int hk) {
        this.limit = k;
        this.hlimit = hk;
    }

    @Override
    protected Context selectNewObjContext(CSMethod method, NewObj obj) {
        return factory.getLastK(method.getContext(), hlimit);
    }
}
