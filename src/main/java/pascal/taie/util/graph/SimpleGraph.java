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

import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Sets;

import java.util.Collections;
import java.util.Set;

/**
 * A simple map-based implementation of {@link Graph<N>}.
 *
 * @param <N> type of nodes
 */
public class SimpleGraph<N> implements Graph<N> {

    private final Set<N> nodes = Sets.newSet();

    private final MultiMap<N, N> preds = Maps.newMultiMap();

    private final MultiMap<N, N> succs = Maps.newMultiMap();

    public void addNode(N node) {
        nodes.add(node);
    }

    public void addEdge(N source, N target) {
        nodes.add(source);
        nodes.add(target);
        preds.put(target, source);
        succs.put(source, target);
    }

    @Override
    public boolean hasNode(N node) {
        return nodes.contains(node);
    }

    @Override
    public boolean hasEdge(N source, N target) {
        return succs.get(source).contains(target);
    }

    @Override
    public Set<N> getPredsOf(N node) {
        return preds.get(node);
    }

    @Override
    public Set<N> getSuccsOf(N node) {
        return succs.get(node);
    }

    @Override
    public int getInDegreeOf(N node) {
        return preds.get(node).size();
    }

    @Override
    public Set<N> getNodes() {
        return Collections.unmodifiableSet(nodes);
    }
}
