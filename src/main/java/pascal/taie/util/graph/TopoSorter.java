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

import pascal.taie.util.collection.Sets;
import pascal.taie.util.collection.Streams;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Topologically sorts a directed graph using DFS.
 * It is assumed that the given graph is a direct acyclic graph (DAG).
 *
 * @param <N> type of nodes
 */
public class TopoSorter<N> {

    private Graph<N> graph;
    private List<N> sortedList;
    private Set<N> visited;

    public TopoSorter(Graph<N> graph) {
        this(graph, false);
    }

    public TopoSorter(Graph<N> graph, boolean reverse) {
        this(graph, reverse, List.of());
    }

    /**
     * Computes a topological soring of a graph, while the client code
     * wishes to preserve some ordering in the sorting result.
     * If preserved order conflicts the topological order, the latter is respected.
     * @param graph          the graph
     * @param preservedOrder the order of the nodes that the client code
     *                       wishes to preserve
     */
    public TopoSorter(Graph<N> graph, List<N> preservedOrder) {
        this(graph, false, preservedOrder);
    }

    private TopoSorter(Graph<N> graph, boolean reverse, List<N> preservedOrder) {
        initialize(graph);
        preservedOrder.forEach(this::visit);
        graph.nodes()
                .filter(n -> Streams.isEmpty(graph.succsOf(n)))
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
            for (N pred : graph.predsOf(curr).collect(Collectors.toList())) {
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
