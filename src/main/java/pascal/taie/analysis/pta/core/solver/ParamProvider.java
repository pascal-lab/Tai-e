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

package pascal.taie.analysis.pta.core.solver;


import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.language.classes.JField;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.TwoKeyMultiMap;

import java.util.Set;

/**
 * The parameter object provider for this variable/parameters of the entry method.
 * This class also supports supplying objects pointed to by fields
 * of parameter objects, as well as elements of array objects.
 *
 * @see EmptyParamProvider
 * @see DeclaredParamProvider
 * @see SpecifiedParamProvider
 */
public interface ParamProvider {

    /**
     * @return the objects for this variable.
     */
    Set<Obj> getThisObjs();

    /**
     * @return the objects for i-th parameter (starting from 0).
     */
    Set<Obj> getParamObjs(int i);

    /**
     * @return the objects pointed to by parameter objects' fields.
     */
    TwoKeyMultiMap<Obj, JField, Obj> getFieldObjs();

    /**
     * @return the elements contained in parameter arrays.
     */
    MultiMap<Obj, Obj> getArrayObjs();
}
