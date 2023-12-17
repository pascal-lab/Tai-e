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

import pascal.taie.analysis.graph.flowgraph.FlowKind;
import pascal.taie.analysis.pta.core.cs.element.Pointer;

/**
 * Base class for call edges of {@link FlowKind#OTHER}.
 * Implementation of {@link FlowKind#OTHER} flow edges should inherit this class.
 */
public abstract class OtherEdge extends PointerFlowEdge {

    protected OtherEdge(Pointer source, Pointer target) {
        super(FlowKind.OTHER, source, target);
    }

    /**
     * @return String representation of information for this edge.
     * It contains simple name of the edge class to distinguish
     * from other classes of {@link FlowKind#OTHER} flow edges.
     */
    @Override
    public String getInfo() {
        return kind().name() + "." + getClass().getSimpleName();
    }
}
