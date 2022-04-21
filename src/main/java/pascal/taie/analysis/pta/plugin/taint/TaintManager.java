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

package pascal.taie.analysis.pta.plugin.taint;

import pascal.taie.analysis.pta.core.heap.MockObj;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.type.Type;
import pascal.taie.util.AnalysisException;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.TwoKeyMap;

/**
 * Manages taint objects.
 */
class TaintManager {

    private static final String TAINT_DESC = "TaintObj";

    private final TwoKeyMap<Invoke, Type, Obj> taints = Maps.newTwoKeyMap();

    /**
     * Makes a taint object for given source and type.
     *
     * @param source invocation to the source method, i.e., source call
     * @param type   type of the taint object
     * @return the taint object for given source and type.
     */
    Obj makeTaint(Invoke source, Type type) {
        return taints.computeIfAbsent(source, type,
                (s, t) -> new MockObj(TAINT_DESC, s, t));
    }

    /**
     * @return true if given obj represents a taint object, otherwise false.
     */
    boolean isTaint(Obj obj) {
        return obj instanceof MockObj &&
                ((MockObj) obj).getDescription().equals(TAINT_DESC);
    }

    /**
     * @return the source call of given taint object.
     * @throws AnalysisException if given object is not a taint object.
     */
    Invoke getSourceCall(Obj obj) {
        if (isTaint(obj)) {
            return (Invoke) obj.getAllocation();
        }
        throw new AnalysisException(obj + " is not a taint object");
    }
}
