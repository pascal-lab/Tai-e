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

import pascal.taie.util.Indexable;
import pascal.taie.util.collection.Sets;

import java.util.Set;

abstract class OFGNode implements Indexable {

    private final int index;

    private Set<OFGEdge> inEdges = Set.of();

    private Set<OFGEdge> outEdges = Set.of();

    OFGNode(int index) {
        this.index = index;
    }

    @Override
    public int getIndex() {
        return index;
    }

    void addOutEdge(OFGEdge edge) {
        if (outEdges.isEmpty()) {
            outEdges = Sets.newHybridSet();
        }
        outEdges.add(edge);
        edge.target().addInEdge(edge);
    }

    private void addInEdge(OFGEdge edge) {
        if (inEdges.isEmpty()) {
            inEdges = Sets.newHybridSet();
        }
        inEdges.add(edge);
    }

    Set<OFGEdge> getInEdges() {
        return inEdges;
    }

    Set<OFGEdge> getOutEdges() {
        return outEdges;
    }
}
