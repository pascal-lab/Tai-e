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
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.language.classes.JMethod;

import java.util.function.Predicate;

/**
 * Selective context selector which applies context sensitivity
 * for part of methods and objects.
 */
class SelectiveSelector implements ContextSelector {

    /**
     * Delegate context selector.
     */
    private final ContextSelector delegate;

    /**
     * Predicate for whether a method should be analyzed with context sensitivity.
     */
    private final Predicate<JMethod> isCSMethod;

    /**
     * Predicate for whether an object should be analyzed with context sensitivity.
     */
    private final Predicate<Obj> isCSObj;

    SelectiveSelector(ContextSelector delegate,
                      Predicate<JMethod> isCSMethod, Predicate<Obj> isCSObj) {
        this.delegate = delegate;
        this.isCSMethod = isCSMethod;
        this.isCSObj = isCSObj;
    }

    @Override
    public Context getEmptyContext() {
        return delegate.getEmptyContext();
    }

    @Override
    public Context selectContext(CSCallSite callSite, JMethod callee) {
        return isCSMethod.test(callee) ?
                delegate.selectContext(callSite, callee) :
                delegate.getEmptyContext();
    }

    @Override
    public Context selectContext(CSCallSite callSite, CSObj recv, JMethod callee) {
        return isCSMethod.test(callee) ?
                delegate.selectContext(callSite, recv, callee) :
                delegate.getEmptyContext();
    }

    @Override
    public Context selectHeapContext(CSMethod method, Obj obj) {
        return isCSObj.test(obj) ?
                delegate.selectHeapContext(method, obj) :
                delegate.getEmptyContext();
    }
}
