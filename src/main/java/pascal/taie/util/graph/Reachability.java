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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;

import static java.util.function.Predicate.not;

/**
 * Computes reachability information for graph.
 *
 * @param <N> type of graph nodes.
 */
public class Reachability<N> {

    private final Graph<N> graph;

    /**
     * Maps a source node to all nodes reachable from it on the graph.
     */
    private final MultiMap<N, N> source2Reachable = Maps.newMultiMap();

    /**
     * Maps a target node to all nodes that can reach it on the graph.
     */
    private final MultiMap<N, N> target2CanReach = Maps.newMultiMap();

    public Reachability(Graph<N> graph) {
        this.graph = graph;
    }

    /**
     * @return all nodes those can be reached from {@code source}.
     */
    public Set<N> reachableNodesFrom(N source) {
        if (!source2Reachable.containsKey(source)) {
            Set<N> visited = Sets.newSet();
            Deque<N> stack = new ArrayDeque<>();
            stack.push(source);
            while (!stack.isEmpty()) {
                N node = stack.pop();
                if (visited.add(node)) {
                    graph.getSuccsOf(node)
                            .stream()
                            .filter(not(visited::contains))
                            .forEach(stack::push);
                }
            }
            source2Reachable.putAll(source, visited);
        }
        return source2Reachable.get(source);
    }

    /**
     * @return all nodes those can reach {@code target}.
     */
    public Set<N> nodesCanReach(N target) {
        if (!target2CanReach.containsKey(target)) {
            Set<N> visited = Sets.newSet();
            Deque<N> stack = new ArrayDeque<>();
            stack.push(target);
            while (!stack.isEmpty()) {
                N node = stack.pop();
                if (visited.add(node)) {
                    graph.getPredsOf(node)
                            .stream()
                            .filter(not(visited::contains))
                            .forEach(stack::push);
                }
            }
            target2CanReach.putAll(target, visited);
        }
        return target2CanReach.get(target);
    }
}
