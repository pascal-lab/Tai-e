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
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static pascal.taie.util.collection.CollectionUtils.newSet;

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
        initialize(graph);
        graph.nodes()
                .filter(n -> graph.succsOf(n).findAny().isEmpty())
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
        this.visited = newSet();
    }

    private void visit(N node) {
        if (!visited.contains(node)) {
            visited.add(node);
            graph.predsOf(node).forEach(this::visit);
            sortedList.add(node);
        }
    }

    private void clear() {
        // release memory
        graph = null;
        visited = null;
    }
}
