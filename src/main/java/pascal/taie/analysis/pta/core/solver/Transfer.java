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

import pascal.taie.analysis.pta.pts.PointsToSet;

/**
 * Transfer function on pointer flow edges.
 * For a given pointer flow edge "source" -> "target", the function defines
 * how the points-to facts of "source" node are propagated to the "target" node.
 */
@FunctionalInterface
public interface Transfer {

    /**
     * Transfer function on a pointer flow edge.
     *
     * @param edge  the pointer flow edge being transferred.
     * @param input set of objects pointed to by the "source" node.
     * @return set of objects that are propagated to the "target" node.
     */
    PointsToSet apply(PointerFlowEdge edge, PointsToSet input);
}
