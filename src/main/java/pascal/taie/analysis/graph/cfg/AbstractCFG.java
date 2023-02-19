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

package pascal.taie.analysis.graph.cfg;

import pascal.taie.ir.IR;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Sets;
import pascal.taie.util.collection.Views;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Set;

abstract class AbstractCFG<N> implements CFG<N> {

    protected final IR ir;

    protected N entry;

    protected N exit;

    protected final Set<N> nodes;

    private final MultiMap<N, CFGEdge<N>> inEdges;

    private final MultiMap<N, CFGEdge<N>> outEdges;

    AbstractCFG(IR ir) {
        this.ir = ir;
        // number of nodes = number of statements in IR + entry + exit
        int nNodes = ir.getStmts().size() + 2;
        nodes = Sets.newSet(nNodes);
        inEdges = Maps.newMultiMap(nNodes);
        outEdges = Maps.newMultiMap(nNodes);
    }

    @Override
    public IR getIR() {
        return ir;
    }

    @Override
    public JMethod getMethod() {
        return ir.getMethod();
    }

    void setEntry(N entry) {
        assert this.entry == null : "CFG entry should be set only once";
        this.entry = entry;
        nodes.add(entry);
    }

    @Override
    public N getEntry() {
        return entry;
    }

    void setExit(N exit) {
        assert this.exit == null : "CFG exit should be set only once";
        this.exit = exit;
        nodes.add(exit);
    }

    @Override
    public N getExit() {
        return exit;
    }

    @Override
    public boolean isEntry(N node) {
        return node == entry;
    }

    @Override
    public boolean isExit(N node) {
        return node == exit;
    }

    void addNode(N node) {
        nodes.add(node);
    }

    void addEdge(CFGEdge<N> edge) {
        CFGEdge<N> existingEdge;
        if (edge.isExceptional() &&
                (existingEdge = getExistingEdge(edge)) != null) {
            // Merge exceptional edges with the same kind, source, and target
            ((ExceptionalEdge<N>) existingEdge).addExceptions(
                    edge.getExceptions());
        } else {
            inEdges.put(edge.target(), edge);
            outEdges.put(edge.source(), edge);
        }
    }

    /**
     * If this CFG already contains an existing edge with same
     * kind, source, and target of the given edge, returns the existing edge,
     * otherwise returns null.
     */
    @Nullable
    private CFGEdge<N> getExistingEdge(CFGEdge<N> edge) {
        for (CFGEdge<N> outEdge : outEdges.get(edge.source())) {
            if (outEdge.target().equals(edge.target()) &&
                    outEdge.getKind() == edge.getKind()) {
                return outEdge;
            }
        }
        return null;
    }

    @Override
    public Set<CFGEdge<N>> getInEdgesOf(N node) {
        return inEdges.get(node);
    }

    @Override
    public Set<CFGEdge<N>> getOutEdgesOf(N node) {
        return outEdges.get(node);
    }

    @Override
    public Set<N> getPredsOf(N node) {
        return Views.toMappedSet(getInEdgesOf(node), CFGEdge::source);
    }

    @Override
    public Set<N> getSuccsOf(N node) {
        return Views.toMappedSet(getOutEdgesOf(node), CFGEdge::target);
    }

    @Override
    public Set<N> getNodes() {
        return Collections.unmodifiableSet(nodes);
    }
}
