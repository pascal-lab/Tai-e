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

import pascal.taie.util.collection.Sets;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Represents a node in {@link MergedSCCGraph}, where each node
 * corresponds to a SCC.
 *
 * @param <N> type of nodes
 */
public class MergedNode<N> {

    private final List<N> nodes;

    private final Set<MergedNode<N>> preds = Sets.newHybridSet();

    private final Set<MergedNode<N>> succs = Sets.newHybridSet();

    MergedNode(Collection<N> nodes) {
        assert !nodes.isEmpty();
        this.nodes = List.copyOf(nodes);
    }

    void addPred(MergedNode<N> pred) {
        preds.add(pred);
    }

    Set<MergedNode<N>> getPreds() {
        return preds;
    }

    void addSucc(MergedNode<N> succ) {
        succs.add(succ);
    }

    Set<MergedNode<N>> getSuccs() {
        return succs;
    }

    public List<N> getNodes() {
        return nodes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MergedNode<?> that = (MergedNode<?>) o;
        return nodes.equals(that.nodes);
    }

    @Override
    public int hashCode() {
        return nodes.hashCode();
    }

    @Override
    public String toString() {
        return "MergedNode" + nodes;
    }
}
