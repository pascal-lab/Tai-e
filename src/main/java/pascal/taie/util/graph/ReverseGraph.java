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
    public boolean hasNode(N node) {
        return graph.hasNode(node);
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
