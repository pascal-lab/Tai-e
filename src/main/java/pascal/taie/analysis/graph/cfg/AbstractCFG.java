/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2020-- Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020-- Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * Tai-e is only for educational and academic purposes,
 * and any form of commercial use is disallowed.
 * Distribution of Tai-e is disallowed without the approval.
 */

package pascal.taie.analysis.graph.cfg;

import pascal.taie.ir.IR;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Sets;
import pascal.taie.util.collection.Views;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.stream.Stream;

abstract class AbstractCFG<N> implements CFG<N> {

    protected final IR ir;

    protected N entry;

    protected N exit;

    protected final Set<N> nodes;

    private final MultiMap<N, Edge<N>> inEdges;

    private final MultiMap<N, Edge<N>> outEdges;

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

    void addEdge(Edge<N> edge) {
        Edge<N> existingEdge;
        if (edge.isExceptional() &&
                (existingEdge = getExistingEdge(edge)) != null) {
            // Merge exceptional edges with the same kind, source, and target
            ((ExceptionalEdge<N>) existingEdge).addExceptions(
                    edge.exceptions());
        } else {
            inEdges.put(edge.getTarget(), edge);
            outEdges.put(edge.getSource(), edge);
        }
    }

    /**
     * If this CFG already contains an existing edge with same
     * kind, source, and target of the given edge, returns the existing edge,
     * otherwise returns null.
     */
    private @Nullable
    Edge<N> getExistingEdge(Edge<N> edge) {
        for (Edge<N> outEdge : outEdges.get(edge.getSource())) {
            if (outEdge.getTarget().equals(edge.getTarget()) &&
                    outEdge.getKind() == edge.getKind()) {
                return outEdge;
            }
        }
        return null;
    }

    @Override
    public Set<Edge<N>> getInEdgesOf(N node) {
        return inEdges.get(node);
    }

    @Override
    public Stream<Edge<N>> outEdgesOf(N node) {
        return outEdges.get(node).stream();
    }

    @Override
    public int getOutDegreeOf(N node) {
        return outEdges.get(node).size();
    }

    @Override
    public boolean hasNode(N node) {
        return nodes.contains(node);
    }

    @Override
    public boolean hasEdge(N source, N target) {
        return succsOf(source).anyMatch(target::equals);
    }

    @Override
    public Set<N> getPredsOf(N node) {
        return Views.toMappedSet(getInEdgesOf(node), Edge::getSource);
    }

    @Override
    public Stream<N> succsOf(N node) {
        return outEdgesOf(node).map(Edge::getTarget);
    }

    @Override
    public Stream<N> nodes() {
        return nodes.stream();
    }

    @Override
    public int getNumberOfNodes() {
        return nodes.size();
    }
}
