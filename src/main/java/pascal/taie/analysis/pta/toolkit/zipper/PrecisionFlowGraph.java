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

package pascal.taie.analysis.pta.toolkit.zipper;

import pascal.taie.analysis.graph.flowgraph.Node;
import pascal.taie.analysis.graph.flowgraph.VarNode;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Views;
import pascal.taie.util.graph.Graph;

import java.util.Set;
import java.util.stream.Collectors;

class PrecisionFlowGraph implements Graph<Node> {

    private final Type type;

    private final ObjectFlowGraph ofg;

    private final Set<Node> nodes;

    private final Set<VarNode> outNodes;

    private final MultiMap<Node, Edge> inWUEdges;

    private final MultiMap<Node, Edge> outWUEdges;

    PrecisionFlowGraph(Type type, ObjectFlowGraph ofg,
                       Set<Node> nodes, Set<VarNode> outNodes,
                       MultiMap<Node, Edge> outWUEdges) {
        this.type = type;
        this.ofg = ofg;
        this.nodes = nodes;
        this.outNodes = outNodes
                .stream()
                .filter(nodes::contains)
                .collect(Collectors.toUnmodifiableSet());
        this.outWUEdges = outWUEdges;
        this.inWUEdges = Maps.newMultiMap();
        outWUEdges.values()
                .forEach(edge -> inWUEdges.put(edge.target(), edge));
    }

    Type getType() {
        return type;
    }

    Set<VarNode> getOutNodes() {
        return outNodes;
    }

    @Override
    public boolean hasNode(Node node) {
        return nodes.contains(node);
    }

    @Override
    public boolean hasEdge(Node source, Node target) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Node> getPredsOf(Node node) {
        return Views.toMappedSet(getInEdgesOf(node), Edge::source);
    }

    @Override
    public Set<Edge> getInEdgesOf(Node node) {
        Set<Edge> inEdges = ofg.getInEdgesOf(node)
                .stream()
                .filter(e -> nodes.contains(e.source()))
                .collect(Collectors.toSet());
        inEdges.addAll(inWUEdges.get(node));
        return inEdges;
    }

    @Override
    public Set<Node> getSuccsOf(Node node) {
        return Views.toMappedSet(getOutEdgesOf(node), Edge::target);
    }

    @Override
    public Set<Edge> getOutEdgesOf(Node node) {
        Set<Edge> outEdges = ofg.getOutEdgesOf(node)
                .stream()
                .filter(e -> nodes.contains(e.target()))
                .collect(Collectors.toSet());
        outEdges.addAll(outWUEdges.get(node));
        return outEdges;
    }

    @Override
    public Set<Node> getNodes() {
        return nodes;
    }
}
