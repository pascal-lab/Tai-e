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

import pascal.taie.analysis.pta.toolkit.PointerAnalysisResultEx;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ReferenceType;

import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

class PFGBuilder {

    private final PointerAnalysisResultEx pta;

    private final ObjectFlowGraph ofg;

    private final ObjectAllocationGraph oag;

    private final PotentialContextElement pce;

    /**
     * The input type.
     */
    private final ReferenceType type;

    /**
     * The current precision flow graph being built.
     */
    private PrecisionFlowGraph pfg;

    PFGBuilder(PointerAnalysisResultEx pta, ObjectFlowGraph ofg,
               ObjectAllocationGraph oag, PotentialContextElement pce,
               ReferenceType type) {
        this.pta = pta;
        this.ofg = ofg;
        this.oag = oag;
        this.pce = pce;
        this.type = type;
    }

    PrecisionFlowGraph build() {
        pfg = new PrecisionFlowGraph(ofg);
        return pfg;
    }

    private Set<JMethod> obtainMethods() {
        return pta.getObjectsOf(type)
            .stream()
            .map(pta::getMethodsInvokedOn)
            .flatMap(Set::stream)
            .collect(Collectors.toUnmodifiableSet());
    }
}
