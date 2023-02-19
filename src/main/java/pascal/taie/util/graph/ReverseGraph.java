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

package pascal.taie.util.graph;

import java.util.Set;

/**
 * A reverse view of given graph.
 *
 * @param <N> type of nodes.
 */
public class ReverseGraph<N> implements Graph<N> {

    private final Graph<N> graph;

    public ReverseGraph(Graph<N> graph) {
        this.graph = graph;
    }

    @Override
    public boolean hasEdge(N source, N target) {
        return graph.hasEdge(target, source);
    }

    @Override
    public Set<N> getPredsOf(N node) {
        return graph.getSuccsOf(node);
    }

    @Override
    public Set<N> getSuccsOf(N node) {
        return graph.getPredsOf(node);
    }

    @Override
    public Set<N> getNodes() {
        return graph.getNodes();
    }
}
