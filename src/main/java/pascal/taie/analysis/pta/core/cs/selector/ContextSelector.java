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

/**
 * Represents context sensitivity variants.
 */
public interface ContextSelector {

    /**
     * @return the empty context that does not contain any context elements.
     */
    Context getEmptyContext();

    /**
     * Selects contexts for static methods.
     *
     * @param callSite the (context-sensitive) call site.
     * @param callee   the callee.
     * @return the context for the callee.
     */
    Context selectContext(CSCallSite callSite, JMethod callee);

    /**
     * Selects contexts for instance methods.
     *
     * @param callSite the (context-sensitive) call site.
     * @param recv     the (context-sensitive) receiver object for the callee.
     * @param callee   the callee.
     * @return the context for the callee.
     */
    Context selectContext(CSCallSite callSite, CSObj recv, JMethod callee);

    /**
     * Selects heap contexts for new-created abstract objects.
     *
     * @param method the (context-sensitive) method that contains the
     *               allocation site of the new-created object.
     * @param obj    the new-created object.
     * @return the heap context for the object.
     */
    Context selectHeapContext(CSMethod method, Obj obj);
}
