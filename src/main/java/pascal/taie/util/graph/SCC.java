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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import static pascal.taie.util.collection.CollectionUtils.newMap;

/**
 * Finding strongly connected components in a directed graph
 * using Tarjan's algorithm.
 *
 * @param <N> type of nodes
 */
public class SCC<N> {

    private final List<List<N>> componentList = new ArrayList<>();
    private final List<List<N>> trueComponentList = new ArrayList<>();

    private Map<N, Integer> indexForNode = newMap();
    private Map<N, Integer> lowlinkForNode = newMap();
    private Stack<N> stack = new Stack<>();

    private int index = 0;

    private Graph<N> graph;

    public SCC(Graph<N> graph) {
        this.graph = graph;
        graph.nodes().forEach(node -> {
            if (!indexForNode.containsKey(node)) {
                recurse(node);
            }
        });
        validate(graph, componentList);
        clear();
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

    private void recurse(N node) {
        indexForNode.put(node, index);
        lowlinkForNode.put(node, index);
        ++index;
        stack.push(node);
        graph.succsOf(node).forEach(succ -> {
            if (!indexForNode.containsKey(succ)) {
                recurse(succ);
                lowlinkForNode.put(node, Math.min(lowlinkForNode.get(node),
                        lowlinkForNode.get(succ)));
            } else if (stack.contains(succ)) {
                lowlinkForNode.put(node, Math.min(lowlinkForNode.get(node),
                        indexForNode.get(succ)));
            }
        });
        if (lowlinkForNode.get(node).intValue() ==
                indexForNode.get(node).intValue()) {
            List<N> scc = new ArrayList<>();
            N v2;
            do {
                v2 = stack.pop();
                scc.add(v2);
            } while (node != v2);
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
    }

    /**
     * Validate whether the number of nodes in all SCCs is
     * equal to the number of nodes in the given graph.
     */
    private void validate(Graph<N> graph, List<List<N>> SCCs) {
        assert graph.getNumberOfNodes() ==
                SCCs.stream().mapToInt(List::size).sum();
    }

    private void clear() {
        // release memory
        indexForNode = null;
        lowlinkForNode = null;
        stack = null;
        this.graph = null;
    }
}
