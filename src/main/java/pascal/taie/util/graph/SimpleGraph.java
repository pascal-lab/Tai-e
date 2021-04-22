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

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static pascal.taie.util.collection.CollectionUtils.addToMapSet;
import static pascal.taie.util.collection.CollectionUtils.newMap;
import static pascal.taie.util.collection.CollectionUtils.newSet;

/**
 * A simple map-based implementation of {@link Graph<N>}.
 * @param <N> type of nodes
 */
public class SimpleGraph<N> implements Graph<N> {

    private final Set<N> nodes = newSet();

    private final Map<N, Set<N>> predMap = newMap();

    private final Map<N, Set<N>> succMap = newMap();

    public void addNode(N node) {
        nodes.add(node);
    }

    public void addEdge(N source, N target) {
        nodes.add(source);
        nodes.add(target);
        addToMapSet(predMap, target, source);
        addToMapSet(succMap, source, target);
    }

    @Override
    public boolean hasNode(N node) {
        return nodes.contains(node);
    }

    @Override
    public boolean hasEdge(N source, N target) {
        return succMap.getOrDefault(source, Collections.emptySet())
                .contains(target);
    }

    @Override
    public Stream<N> predsOf(N node) {
        return predMap.getOrDefault(node, Collections.emptySet()).stream();
    }

    @Override
    public Stream<N> succsOf(N node) {
        return succMap.getOrDefault(node, Collections.emptySet()).stream();
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
