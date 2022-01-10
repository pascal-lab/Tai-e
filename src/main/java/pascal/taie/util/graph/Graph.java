/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2020-- Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020-- Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * Tai-e is only for educational and academic purposes,
 * and any form of commercial use is disallowed.
 * Distribution of Tai-e is disallowed without the approval.
 */

package pascal.taie.util.graph;

import pascal.taie.util.collection.Views;

import java.util.Iterator;
import java.util.Set;
import java.util.stream.Stream;

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
    Stream<N> nodes();

    /**
     * @return the number of the nodes in this graph.
     */
    int getNumberOfNodes();

    @Override
    default Iterator<N> iterator() {
        return nodes().iterator();
    }
}
