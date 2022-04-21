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

import pascal.taie.util.collection.Views;

import java.util.Iterator;
import java.util.Set;

/**
 * Representation of a directed graph.
 *
 * @param <N> type of nodes
 */
public interface Graph<N> extends Iterable<N> {

    /**
     * @return true if this graph has given node, otherwise false.
     */
    boolean hasNode(N node);

    /**
     * @return true if this graph has an edge from given source to target,
     * otherwise false.
     */
    boolean hasEdge(N source, N target);

    /**
     * @return true if this graph has the given edge, otherwise false.
     */
    default boolean hasEdge(Edge<N> edge) {
        return hasEdge(edge.getSource(), edge.getTarget());
    }

    /**
     * @return the predecessors of given node in this graph.
     */
    Set<N> getPredsOf(N node);

    /**
     * @return the successors of given node in this graph.
     */
    Set<N> getSuccsOf(N node);

    /**
     * @return incoming edges of the given node.
     */
    default Set<? extends Edge<N>> getInEdgesOf(N node) {
        return Views.toMappedSet(getPredsOf(node),
                pred -> new AbstractEdge<N>(pred, node) {});
    }

    /**
     * @return the number of in edges of the given node.
     */
    default int getInDegreeOf(N node) {
        return getInEdgesOf(node).size();
    }

    /**
     * @return outgoing edges of the given node.
     */
    default Set<? extends Edge<N>> getOutEdgesOf(N node) {
        return Views.toMappedSet(getSuccsOf(node),
                succ -> new AbstractEdge<N>(node, succ) {});
    }

    /**
     * @return the number of out edges of the given node.
     */
    default int getOutDegreeOf(N node) {
        return getOutEdgesOf(node).size();
    }

    /**
     * @return all nodes of this graph.
     */
    Set<N> getNodes();

    /**
     * @return the number of the nodes in this graph.
     */
    default int getNumberOfNodes() {
        return getNodes().size();
    }

    @Override
    default Iterator<N> iterator() {
        return getNodes().iterator();
    }
}
