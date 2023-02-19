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

package pascal.taie.analysis.pta.core.solver;

import pascal.taie.analysis.pta.core.cs.element.Pointer;
import pascal.taie.util.collection.Sets;
import pascal.taie.util.collection.Views;
import pascal.taie.util.graph.Edge;
import pascal.taie.util.graph.Graph;

import java.util.Collections;
import java.util.Set;

/**
 * Represents pointer flow graph in context-sensitive pointer analysis.
 */
public class PointerFlowGraph implements Graph<Pointer> {

    private final Set<Pointer> pointers = Sets.newSet();

    public boolean addEdge(PointerFlowEdge edge) {
        if (edge.source().addOutEdge(edge)) {
            pointers.add(edge.source());
            pointers.add(edge.target());
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Set<? extends Edge<Pointer>> getInEdgesOf(Pointer node) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<PointerFlowEdge> getOutEdgesOf(Pointer pointer) {
        return pointer.getOutEdges();
    }

    public Set<Pointer> getPointers() {
        return Collections.unmodifiableSet(pointers);
    }

    @Override
    public Set<Pointer> getPredsOf(Pointer node) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Pointer> getSuccsOf(Pointer node) {
        return Views.toMappedSet(node.getOutEdges(),
                PointerFlowEdge::target);
    }

    @Override
    public Set<Pointer> getNodes() {
        return getPointers();
    }
}
