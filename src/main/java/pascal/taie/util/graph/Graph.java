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

import java.util.Iterator;
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
    Stream<N> predsOf(N node);

    /**
     * @return the successors of given node in this graph.
     */
    Stream<N> succsOf(N node);

    /**
     * @return incoming edges of the given node.
     */
    default Stream<? extends Edge<N>> inEdgesOf(N node) {
        return predsOf(node)
                .map(pred -> new AbstractEdge<N>(pred, node) {});
    }

    /**
     * @return outgoing edges of the given node.
     */
    default Stream<? extends Edge<N>> outEdgesOf(N node) {
        return succsOf(node)
                .map(succ -> new AbstractEdge<N>(node, succ) {});
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
