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

import pascal.taie.language.type.Type;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Views;
import pascal.taie.util.graph.SimpleGraph;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

class PrecisionFlowGraph extends SimpleGraph<OFGNode> {

    private final Type type;

    private Set<VarNode> outNodes;

    private final MultiMap<OFGNode, OFGEdge> outEdges = Maps.newMultiMap();

    PrecisionFlowGraph(Type type) {
        this.type = type;
    }

    Type getType() {
        return type;
    }

    void setOutNodes(Collection<VarNode> outNodes) {
        this.outNodes = outNodes.stream()
            .filter(this::hasNode)
            .collect(Collectors.toUnmodifiableSet());
    }

    Set<VarNode> getOutNodes() {
        return outNodes;
    }

    void addEdge(OFGEdge edge) {
        addEdge(edge.source(), edge.target());
        outEdges.put(edge.source(), edge);
    }

    @Override
    public Set<OFGEdge> getOutEdgesOf(OFGNode node) {
        return outEdges.get(node);
    }

    @Override
    public Set<OFGNode> getSuccsOf(OFGNode node) {
        return Views.toMappedSet(getOutEdgesOf(node), OFGEdge::target);
    }
}
