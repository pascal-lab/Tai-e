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

package pascal.taie.analysis.graph.icfg;

import pascal.taie.analysis.graph.cfg.Edge;

/**
 * The edge connecting nodes in the same method.
 * Note that This kind of edges does not include the edges from call sites
 * to their return sites, which are represented by {@link CallToReturnEdge}.
 *
 * @param <Node> type of nodes
 */
public class NormalEdge<Node> extends ICFGEdge<Node> {

    /**
     * The corresponding CFG edge, which brings the information of edge type.
     */
    private final Edge<Node> cfgEdge;

    NormalEdge(Edge<Node> edge) {
        super(edge.getSource(), edge.getTarget());
        this.cfgEdge = edge;
    }

    public Edge<Node> getCFGEdge() {
        return cfgEdge;
    }
}
