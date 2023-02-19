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
import pascal.taie.util.collection.Sets;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Represents a merged graph of a directed graph G.
 * Each SCC of G is represented by a merged node of this graph.
 *
 * @see MergedNode
 */
public class MergedSCCGraph<N> implements Graph<MergedNode<N>> {

    private Set<MergedNode<N>> nodes;

    public MergedSCCGraph(Graph<N> graph) {
        init(graph);
    }

    private void init(Graph<N> graph) {
        nodes = Sets.newSet();
        // Map from original node to the corresponding merged node.
        Map<N, MergedNode<N>> nodeMap = Maps.newMap(graph.getNumberOfNodes());
        SCC<N> scc = new SCC<>(graph);
        scc.getComponents().forEach(component -> {
            MergedNode<N> node = new MergedNode<>(component);
            component.forEach(n -> nodeMap.put(n, node));
            nodes.add(node);
        });
        nodes.forEach(node -> node.getNodes()
                .stream()
                .flatMap(n -> graph.getSuccsOf(n).stream())
                .map(nodeMap::get)
                .filter(succ -> succ != node) // exclude self-loop
                .forEach(succ -> {
                    node.addSucc(succ);
                    succ.addPred(node);
                }));
    }

    @Override
    public Set<MergedNode<N>> getPredsOf(MergedNode<N> node) {
        return Collections.unmodifiableSet(node.getPreds());
    }

    @Override
    public Set<MergedNode<N>> getSuccsOf(MergedNode<N> node) {
        return Collections.unmodifiableSet(node.getSuccs());
    }

    @Override
    public Set<MergedNode<N>> getNodes() {
        return Collections.unmodifiableSet(nodes);
    }
}
