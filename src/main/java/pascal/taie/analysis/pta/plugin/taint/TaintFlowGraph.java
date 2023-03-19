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

package pascal.taie.analysis.pta.plugin.taint;

import pascal.taie.analysis.graph.flowgraph.FlowEdge;
import pascal.taie.analysis.graph.flowgraph.Node;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Sets;
import pascal.taie.util.collection.Views;
import pascal.taie.util.graph.Graph;

import java.util.Set;

class TaintFlowGraph implements Graph<Node> {

    private final Set<Node> sourceNodes;

    private final Set<Node> sinkNodes;

    private final Set<Node> nodes = Sets.newHybridSet();

    private final MultiMap<Node, FlowEdge> inEdges = Maps.newMultiMap();

    private final MultiMap<Node, FlowEdge> outEdges = Maps.newMultiMap();

    TaintFlowGraph(Set<Node> sourceNodes, Set<Node> sinkNodes) {
        this.sourceNodes = Set.copyOf(sourceNodes);
        nodes.addAll(sourceNodes);
        this.sinkNodes = Set.copyOf(sinkNodes);
        nodes.addAll(sinkNodes);
    }

    Set<Node> getSourceNodes() {
        return sourceNodes;
    }

    Set<Node> getSinkNodes() {
        return sinkNodes;
    }

    void addEdge(FlowEdge edge) {
        nodes.add(edge.source());
        nodes.add(edge.target());
        inEdges.put(edge.target(), edge);
        outEdges.put(edge.source(), edge);
    }

    @Override
    public Set<Node> getPredsOf(Node node) {
        return Views.toMappedSet(getInEdgesOf(node), FlowEdge::source);
    }

    @Override
    public Set<FlowEdge> getInEdgesOf(Node node) {
        return inEdges.get(node);
    }

    @Override
    public Set<Node> getSuccsOf(Node node) {
        return Views.toMappedSet(getOutEdgesOf(node), FlowEdge::target);
    }

    @Override
    public Set<FlowEdge> getOutEdgesOf(Node node) {
        return outEdges.get(node);
    }

    @Override
    public Set<Node> getNodes() {
        return nodes;
    }
}
