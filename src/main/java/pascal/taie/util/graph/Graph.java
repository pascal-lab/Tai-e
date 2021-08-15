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
 * @param <N> type of nodes
 */
public interface Graph<N> extends Iterable<N> {

    /**
     * @return if this graph has given node.
     */
    boolean hasNode(N node);

    /**
     * @return if this graph has an edge from given source to target.
     */
    boolean hasEdge(N source, N target);

    /**
     * @return a stream of predecessors of given node in this graph.
     */
    Stream<N> predsOf(N node);

    /**
     * @return a stream of successors of given node in this graph.
     */
    Stream<N> succsOf(N node);

    /**
     * @return a stream of all nodes of this graph.
     */
    Stream<N> nodes();

    int getNumberOfNodes();

    @Override
    default Iterator<N> iterator() {
        return nodes().iterator();
    }
}
