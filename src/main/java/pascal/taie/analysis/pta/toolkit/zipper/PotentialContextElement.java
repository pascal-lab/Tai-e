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

package pascal.taie.analysis.pta.toolkit.zipper;

import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.toolkit.PointerAnalysisResultEx;
import pascal.taie.analysis.pta.toolkit.util.OAGs;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.SetCanonicalizer;
import pascal.taie.util.collection.Sets;

import java.util.Map;
import java.util.Set;

/**
 * For each object type t, this class compute the set of methods
 * which objects of t could potentially be their context element.
 */
class PotentialContextElement {

    /**
     * Map from each type to PCE methods of the objects of the type.
     */
    private final Map<Type, Set<JMethod>> type2PCEMethods = Maps.newMap();

    PotentialContextElement(PointerAnalysisResultEx pta,
                            ObjectAllocationGraph oag) {
        Map<Obj, Set<JMethod>> invokedMethods = OAGs.computeInvokedMethods(pta);
        SetCanonicalizer<JMethod> canonicalizer = new SetCanonicalizer<>();
        pta.getBase().getObjects()
            .stream()
            .map(Obj::getType)
            .distinct()
            .forEach(type -> {
                Set<JMethod> methods = Sets.newHybridSet();
                // add invoked methods on objects of type
                pta.getObjectsOf(type)
                    .stream()
                    .map(invokedMethods::get)
                    .forEach(methods::addAll);
                // add invoked methods on allocated objects of type
                oag.getAllocateesOf(type)
                    .stream()
                    .map(invokedMethods::get)
                    .forEach(methods::addAll);
                type2PCEMethods.put(type, canonicalizer.get(methods));
            });
    }

    Set<JMethod> PCEMethodsOf(Type type) {
        return type2PCEMethods.get(type);
    }
}
