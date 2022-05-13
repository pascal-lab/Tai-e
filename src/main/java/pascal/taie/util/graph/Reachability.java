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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public class Reachability<N> {

    private final Graph<N> graph;

    private final Map<N, Set<N>> reachableNodes = new HashMap<>();

    private final Map<N, Set<N>> reachToNodes = new HashMap<>();

    public Reachability(Graph<N> graph) {
        this.graph = graph;
    }

    /**
     * @return all nodes that can be reached from {@code source} on the graph.
     */
    public Set<N> reachableNodesFrom(N source) {
        if (!reachableNodes.containsKey(source)) {
            Set<N> visited = new HashSet<>();
            Deque<N> stack = new ArrayDeque<>();
            stack.push(source);
            while (!stack.isEmpty()) {
                N node = stack.pop();
                visited.add(node);
                graph.getSuccsOf(node)
                    .stream()
                    .filter(Predicate.not(visited::contains))
                    .forEach(stack::push);
            }
            reachableNodes.put(source, visited);
        }
        return reachableNodes.get(source);
    }

    /**
     * @return all nodes that can reach {@code target} on the graph.
     */
    public Set<N> nodesReach(N target) {
        if (!reachToNodes.containsKey(target)) {
            Set<N> visited = new HashSet<>();
            Deque<N> stack = new ArrayDeque<>();
            stack.push(target);
            while (!stack.isEmpty()) {
                N node = stack.pop();
                visited.add(node);
                graph.getPredsOf(node)
                    .stream()
                    .filter(Predicate.not(visited::contains))
                    .forEach(stack::push);
            }
            reachToNodes.put(target, visited);
        }
        return reachToNodes.get(target);
    }
}
