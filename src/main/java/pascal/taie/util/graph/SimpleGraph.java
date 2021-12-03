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

import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Sets;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * A simple map-based implementation of {@link Graph<N>}.
 *
 * @param <N> type of nodes
 */
public class SimpleGraph<N> implements Graph<N> {

    private final Set<N> nodes = Sets.newSet();

    private final Map<N, Set<N>> predMap = Maps.newMap();

    private final Map<N, Set<N>> succMap = Maps.newMap();

    public void addNode(N node) {
        nodes.add(node);
    }

    public void addEdge(N source, N target) {
        nodes.add(source);
        nodes.add(target);
        Maps.addToMapSet(predMap, target, source);
        Maps.addToMapSet(succMap, source, target);
    }

    @Override
    public boolean hasNode(N node) {
        return nodes.contains(node);
    }

    @Override
    public boolean hasEdge(N source, N target) {
        return succMap.getOrDefault(source, Set.of()).contains(target);
    }

    @Override
    public Stream<N> predsOf(N node) {
        return predMap.getOrDefault(node, Set.of()).stream();
    }

    @Override
    public Stream<N> succsOf(N node) {
        return succMap.getOrDefault(node, Set.of()).stream();
    }

    @Override
    public int getInDegreeOf(N node) {
        return predMap.getOrDefault(node, Set.of()).size();
    }

    @Override
    public int getOutDegreeOf(N node) {
        return succMap.getOrDefault(node, Set.of()).size();
    }

    @Override
    public Stream<N> nodes() {
        return nodes.stream();
    }

    @Override
    public int getNumberOfNodes() {
        return nodes.size();
    }
}
