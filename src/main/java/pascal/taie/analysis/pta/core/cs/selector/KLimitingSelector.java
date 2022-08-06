/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2022 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2022 Yue Li <yueli@nju.edu.cn>
 *
 * This file is part of Tai-e.
 *
 * Tai-e is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Tai-e is distributed in the hope that it will be useful,but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Tai-e. If not, see <https://www.gnu.org/licenses/>.
 */

package pascal.taie.analysis.pta.core.cs.selector;

import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.heap.NewObj;

/**
 * K-limiting context selector.
 *
 * @param <T> type of context elements.
 */
abstract class KLimitingSelector<T> extends AbstractContextSelector<T> {

    /**
     * Limit of context length.
     */
    protected final int limit;

    /**
     * Limit of heap context length.
     */
    protected final int hLimit;

    /**
     * @param k  k-limit for method contexts.
     * @param hk k-limit for heap contexts.
     */
    KLimitingSelector(int k, int hk) {
        this.limit = k;
        this.hLimit = hk;
    }

    @Override
    protected Context selectNewObjContext(CSMethod method, NewObj obj) {
        return factory.makeLastK(method.getContext(), hLimit);
    }
}
