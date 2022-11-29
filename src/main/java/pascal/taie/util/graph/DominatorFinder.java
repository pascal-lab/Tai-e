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

import pascal.taie.util.Indexer;
import pascal.taie.util.SimpleIndexer;
import pascal.taie.util.collection.HybridBitSet;
import pascal.taie.util.collection.IndexMap;
import pascal.taie.util.collection.IndexerBitSet;
import pascal.taie.util.collection.SetEx;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Finds dominators for the nodes in given {@link Graph}.
 */
public class DominatorFinder<N> {

    /**
     * The graph associated with this finder.
     */
    private final Graph<N> graph;

    /**
     * Indexer for nodes in the graph. This is used to store nodes in bit set.
     */
    private final Indexer<N> indexer;

    /**
     * Head nodes of the graph.
     */
    private final Set<N> heads;

    /**
     * Maps a node N to all its dominators.
     */
    private final Map<N, SetEx<N>> node2Doms;

    /**
     * Maps a node N to all nodes dominated by N.
     * This map is computed on-demand by {@link #findDominatedNodes()}.
     */
    private Map<N, SetEx<N>> dom2Nodes;

    public DominatorFinder(Graph<N> graph) {
        this(graph, new SimpleIndexer<>());
    }

    /**
     * Constructs a dominator finder with a graph and given indexer.
     * Note that {@code indexer} must assign continuous indexes for nodes in
     * {@code graph}, starting from 0, otherwise the finder may throw exception.
     */
    public DominatorFinder(Graph<N> graph, Indexer<N> indexer) {
        this.graph = graph;
        this.indexer = indexer;
        this.heads = new HashSet<>();
        this.node2Doms = new IndexMap<>(indexer, graph.getNumberOfNodes());
        findDominators();
    }

    private void findDominators() {
        // build full set
        SetEx<N> fullSet = new IndexerBitSet<>(indexer, false);
        fullSet.addAll(graph.getNodes());
        // initialize dominators
        Deque<N> workList = new ArrayDeque<>();
        for (N node : graph) {
            SetEx<N> doms;
            if (graph.getInDegreeOf(node) == 0) {
                // node is head
                heads.add(node);
                // head nodes are only dominated by themselves
                doms = new HybridBitSet<>(indexer, false);
                doms.add(node);
            } else {
                // other nodes are initially dominated by all nodes
                doms = fullSet.copy();
                // add to work-list for further processing
                workList.add(node);
            }
            node2Doms.put(node, doms);
        }
        // process work-list
        while (!workList.isEmpty()) {
            N node = workList.pop();
            SetEx<N> oldDoms = node2Doms.get(node);
            SetEx<N> newDoms = fullSet.copy();
            // intersect dominators of all predecessors
            for (N pred : graph.getPredsOf(node)) {
                newDoms.retainAll(node2Doms.get(pred));
            }
            // each node dominates itself
            newDoms.add(node);
            if (!oldDoms.equals(newDoms)) {
                node2Doms.put(node, newDoms);
                workList.addAll(graph.getSuccsOf(node));
            }
        }
    }

    /**
     * @return the dominators of {@code node}.
     */
    public Set<N> getDominatorsOf(N node) {
        return Collections.unmodifiableSet(node2Doms.get(node));
    }

    /**
     * @return the nodes dominated by the {@code dominator}.
     */
    public Set<N> getNodesDominatedBy(N dominator) {
        if (dom2Nodes == null) {
            findDominatedNodes();
        }
        return Collections.unmodifiableSet(dom2Nodes.get(dominator));
    }

    private void findDominatedNodes() {
        dom2Nodes = new IndexMap<>(indexer, graph.getNumberOfNodes());
        for (N node : graph) {
            for (N dom : node2Doms.get(node)) {
                dom2Nodes.computeIfAbsent(dom, d -> new IndexerBitSet<>(indexer, false))
                        .add(node);
            }
        }
    }

    /**
     * @return {@code true} if {@code dominator} is a dominator of {@code node}.
     */
    public boolean isDominatedBy(N node, N dominator) {
        return node2Doms.get(node).contains(dominator);
    }
}
