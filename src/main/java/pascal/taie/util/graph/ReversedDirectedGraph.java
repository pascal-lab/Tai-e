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

import soot.toolkits.graph.DirectedGraph;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.List;

public class ReversedDirectedGraph<N> implements DirectedGraph<N> {

    private final DirectedGraph<N> graph;

    public ReversedDirectedGraph(DirectedGraph<N> graph) {
        this.graph = graph;
    }

    @Override
    public List<N> getHeads() {
        return graph.getTails();
    }

    @Override
    public List<N> getTails() {
        return graph.getHeads();
    }

    @Override
    public List<N> getPredsOf(N s) {
        return graph.getSuccsOf(s);
    }

    @Override
    public List<N> getSuccsOf(N s) {
        return graph.getPredsOf(s);
    }

    @Override
    public int size() {
        return graph.size();
    }

    @Nonnull
    @Override
    public Iterator<N> iterator() {
        return graph.iterator();
    }
}
