/*
 * Tai-e: A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Tai-e is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.taie.analysis.dataflow.solver;

import pascal.taie.analysis.graph.cfg.CFG;
import pascal.taie.util.graph.Graph;
import pascal.taie.util.graph.MergedNode;
import pascal.taie.util.graph.MergedSCCGraph;
import pascal.taie.util.graph.ReverseGraph;
import pascal.taie.util.graph.TopoSorter;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides order for processing nodes in data-flow analysis via
 * topological sorting. CFG may contain cycles, thus we merge SCCs
 * in the CFG and perform topological sorting on the merged graph.
 * For backward analysis, we compute topological order on the reverse CFG.
 *
 * @param <N> type of nodes to be compared
 */
class Orderer<N> implements Comparator<N> {

    private final Map<N, Integer> orders;

    Orderer(CFG<N> cfg, boolean isForward) {
        orders = new LinkedHashMap<>(cfg.getNumberOfNodes());
        init(cfg, isForward);
    }

    private void init(CFG<N> cfg, boolean isForward) {
        Graph<N> g = isForward ? cfg : new ReverseGraph<>(cfg);
        MergedSCCGraph<N> mg = new MergedSCCGraph<>(g);
        List<MergedNode<N>> topoList = new TopoSorter<>(mg).get();
        int order = 0;
        for (MergedNode<N> mergedNode : topoList) {
            for (N node : mergedNode.getNodes()) {
                orders.put(node, order++);
            }
        }
    }

    @Override
    public int compare(N n1, N n2) {
        return orders.get(n1) - orders.get(n2);
    }
}
