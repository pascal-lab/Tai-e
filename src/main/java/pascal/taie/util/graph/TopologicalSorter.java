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

import pascal.taie.util.collection.Sets;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Set;

/**
 * Topologically sorts a directed graph using DFS.
 * It is assumed that the given graph is a direct acyclic graph (DAG).
 *
 * @param <N> type of nodes
 */
public class TopologicalSorter<N> {

    private Graph<N> graph;
    private List<N> sortedList;
    private Set<N> visited;

    public TopologicalSorter(Graph<N> graph) {
        this(graph, false);
    }

    public TopologicalSorter(Graph<N> graph, boolean reverse) {
        this(graph, reverse, List.of());
    }

    /**
     * Computes a topological soring of a graph, while the client code
     * wishes to preserve some ordering in the sorting result.
     * If preserved order conflicts the topological order, the latter is respected.
     *
     * @param graph          the graph
     * @param preservedOrder the order of the nodes that the client code
     *                       wishes to preserve
     */
    public TopologicalSorter(Graph<N> graph, List<N> preservedOrder) {
        this(graph, false, preservedOrder);
    }

    private TopologicalSorter(Graph<N> graph, boolean reverse, List<N> preservedOrder) {
        initialize(graph);
        preservedOrder.forEach(this::visit);
        graph.getNodes()
                .stream()
                .filter(n -> graph.getOutDegreeOf(n) == 0)
                .forEach(this::visit);
        if (reverse) {
            Collections.reverse(sortedList);
        }
        clear();
    }

    /**
     * @return the topologically sorted list.
     */
    public List<N> get() {
        return sortedList;
    }

    private void initialize(Graph<N> graph) {
        this.graph = graph;
        this.sortedList = new ArrayList<>(graph.getNumberOfNodes());
        this.visited = Sets.newSet(graph.getNumberOfNodes());
    }

    private void visit(N node) {
        // use iterative (non-recursive) algorithm to avoid stack overflow
        // for large graph
        if (visited.contains(node)) {
            return;
        }
        Deque<N> stack = new ArrayDeque<>();
        stack.push(node);
        while (!stack.isEmpty()) {
            N curr = stack.peek();
            visited.add(curr);
            boolean hasUnvisitedPred = false;
            for (N pred : graph.getPredsOf(curr)) {
                if (!visited.contains(pred)) {
                    stack.push(pred);
                    hasUnvisitedPred = true;
                    break;
                }
            }
            if (!hasUnvisitedPred) {
                sortedList.add(curr);
                stack.pop();
            }
        }
    }

    private void clear() {
        // release memory
        graph = null;
        visited = null;
    }
}
