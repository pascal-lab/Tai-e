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

package pascal.taie.analysis.pta.toolkit.scaler;

import pascal.taie.analysis.pta.toolkit.PointerAnalysisResultEx;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.Maps;

import java.util.Map;

/**
 * This class computes (estimates) the number of contexts for given method
 * when using corresponding context sensitivity variant.
 */
abstract class ContextComputer {

    final PointerAnalysisResultEx pta;

    /**
     * Map from a method to its context number.
     */
    final Map<JMethod, Integer> method2ctxNumber = Maps.newMap();

    ContextComputer(PointerAnalysisResultEx pta) {
        this.pta = pta;
    }

    /**
     * @return the number of contexts of the given method.
     */
    int contextNumberOf(JMethod method) {
        return method2ctxNumber.computeIfAbsent(
                method, this::computeContextNumberOf);
    }

    /**
     * @return name of the context sensitivity variant.
     */
    abstract String getVariantName();

    /**
     * Computes (estimates) the number of contexts for the given method
     * using the context sensitivity variant.
     */
    abstract int computeContextNumberOf(JMethod method);
}
