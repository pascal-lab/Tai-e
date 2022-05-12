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

import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Views;
import pascal.taie.util.graph.SimpleGraph;

import java.util.HashSet;
import java.util.Set;

class PrecisionFlowGraph extends SimpleGraph<OFGNode> {

    private final ObjectFlowGraph ofg;

    private final MultiMap<OFGNode, OFGEdge> wuEdges = Maps.newMultiMap();

    PrecisionFlowGraph(ObjectFlowGraph ofg) {
        this.ofg = ofg;
    }

    @Override
    public Set<OFGNode> getSuccsOf(OFGNode node) {
        return Views.toMappedSet(getOutEdgesOf(node), OFGEdge::getTarget);
    }

    @Override
    public Set<OFGEdge> getOutEdgesOf(OFGNode node) {
        Set<OFGEdge> outEdges = ofg.getOutEdgesOf(node);
        if (wuEdges.containsKey(node)) {
            outEdges = new HashSet<>(outEdges);
            outEdges.addAll(wuEdges.get(node));
        }
        return outEdges;
    }
}
