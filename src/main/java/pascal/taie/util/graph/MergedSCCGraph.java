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

package pascal.taie.util.graph;

import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Sets;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Represents a merged graph of a directed graph G.
 * Each SCC of G is represented by a merged node of this graph.
 *
 * @see MergedNode
 */
public class MergedSCCGraph<N> implements Graph<MergedNode<N>> {

    private Set<MergedNode<N>> nodes;

    public MergedSCCGraph(Graph<N> graph) {
        init(graph);
    }

    private void init(Graph<N> graph) {
        nodes = Sets.newSet();
        // Map from original node to the corresponding merged node.
        Map<N, MergedNode<N>> nodeMap = Maps.newMap(graph.getNumberOfNodes());
        SCC<N> scc = new SCC<>(graph);
        scc.getComponents().forEach(component -> {
            MergedNode<N> node = new MergedNode<>(component);
            component.forEach(n -> nodeMap.put(n, node));
            nodes.add(node);
        });
        nodes.forEach(node -> node.getNodes()
                .stream()
                .flatMap(n -> graph.getSuccsOf(n).stream())
                .map(nodeMap::get)
                .filter(succ -> succ != node) // exclude self-loop
                .forEach(succ -> {
                    node.addSucc(succ);
                    succ.addPred(node);
                }));
    }

    @Override
    public boolean hasNode(MergedNode<N> node) {
        return nodes.contains(node);
    }

    @Override
    public boolean hasEdge(MergedNode<N> source, MergedNode<N> target) {
        return source.getSuccs().contains(target);
    }

    @Override
    public Set<MergedNode<N>> getPredsOf(MergedNode<N> node) {
        return Collections.unmodifiableSet(node.getPreds());
    }

    @Override
    public Set<MergedNode<N>> getSuccsOf(MergedNode<N> node) {
        return Collections.unmodifiableSet(node.getSuccs());
    }

    @Override
    public Stream<MergedNode<N>> nodes() {
        return nodes.stream();
    }

    @Override
    public int getNumberOfNodes() {
        return nodes.size();
    }
}
