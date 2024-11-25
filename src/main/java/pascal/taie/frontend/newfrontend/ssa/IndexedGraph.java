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

package pascal.taie.frontend.newfrontend.ssa;

import java.util.List;

/**
 * <p>A graph with indexed nodes.</p>
 * <p>This graph must have an entry node.
 * (i.e. for every node n, there is a path from the entry node to n)
 * Because we're handling CFGs, so there may be multiple entry nodes.
 * To fix this, the implementation should insert a new entry node pointing to all entry nodes
 * </p>
 */
public interface IndexedGraph<N> {
    List<N> inEdges(N node);

    List<N> outEdges(N node);

    List<N> normalOutEdges(N node);

    default int getMergedInEdgesCount(int node) {
        return inEdges(getNode(node)).size();
    }

    default int getMergedOutEdgesCount(int node) {
        return outEdges(getNode(node)).size();
    }

    default int getMergedInEdge(int node, int index) {
        return getIndex(inEdges(getNode(node)).get(index));
    }

    default int getMergedOutEdge(int node, int index) {
        return getIndex(outEdges(getNode(node)).get(index));
    }

    N getNode(int index);

    int getIndex(N node);

    int size();

    N getEntry();

    default int getIntEntry() {
        int entry = getIndex(getEntry());
        assert entry == 0;
        return entry;
    }
}
