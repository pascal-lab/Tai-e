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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Finds strongly connected components in a directed graph using
 * Tarjan's algorithm.
 *
 * @param <N> type of nodes
 */
public class SCC<N> {

    private final List<List<N>> componentList = new ArrayList<>();
    private final List<List<N>> trueComponentList = new ArrayList<>();

    public SCC(Graph<N> graph) {
        compute(graph);
        validate(graph, componentList);
    }

    /**
     * @return the list of the strongly-connected components
     */
    public List<List<N>> getComponents() {
        return componentList;
    }

    /**
     * @return the list of the strongly-connected components, but only those
     * that are true components, i.e. components which have more than one element
     * or consists of one node that has itself as a successor
     */
    public List<List<N>> getTrueComponents() {
        return trueComponentList;
    }

    private void compute(Graph<N> graph) {
        // use iterative (non-recursive) algorithm to avoid stack overflow
        // for large graph
        int index = 0;
        Map<N, Integer> indexes = Maps.newMap();
        Map<N, Integer> lows = Maps.newMap();
        Deque<N> stack = new ArrayDeque<>();
        Set<N> inStack = Sets.newSet();
        for (N curr : graph) {
            if (indexes.containsKey(curr)) {
                continue;
            }
            Deque<N> workStack = new ArrayDeque<>();
            workStack.push(curr);
            while (!workStack.isEmpty()) {
                N node = workStack.peek();
                if (!indexes.containsKey(node)) {
                    indexes.put(node, index);
                    lows.put(node, index);
                    ++index;
                    stack.push(node);
                    inStack.add(node);
                }
                boolean hasUnvisitedSucc = false;
                for (N succ : graph.succsOf(node)
                        .collect(Collectors.toList())) {
                    if (!indexes.containsKey(succ)) {
                        workStack.push(succ);
                        hasUnvisitedSucc = true;
                        break;
                    } else if (indexes.get(node) < indexes.get(succ)) {
                        // node->succ is a forward edge
                        lows.put(node, Math.min(lows.get(node), lows.get(succ)));
                    } else if (inStack.contains(succ)) {
                        lows.put(node, Math.min(lows.get(node), indexes.get(succ)));
                    }
                }
                if (!hasUnvisitedSucc) {
                    if (lows.get(node).equals(indexes.get(node))) {
                        collectSCC(node, stack, inStack, graph);
                    }
                    workStack.pop();
                }
            }
        }
    }

    private void collectSCC(N node, Deque<N> stack, Set<N> inStack, Graph<N> graph) {
        List<N> scc = new ArrayList<>();
        N v2;
        do {
            v2 = stack.pop();
            inStack.remove(v2);
            scc.add(v2);
        } while (node != v2);
        // Reverse SCC so that the nodes connected to predecessors
        // (outside of the SCC) will be listed ahead.
        Collections.reverse(scc);
        componentList.add(scc);
        if (scc.size() > 1) {
            trueComponentList.add(scc);
        } else {
            N n = scc.get(0);
            if (graph.hasEdge(n, n)) {
                trueComponentList.add(scc);
            }
        }
    }

    /**
     * Validates whether the number of nodes in all components is
     * equal to the number of nodes in the given graph.
     */
    private void validate(Graph<N> graph, List<List<N>> components) {
        assert graph.getNumberOfNodes() ==
                components.stream().mapToInt(List::size).sum();
    }
}
