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

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static pascal.taie.util.collection.Maps.addToMapSet;
import static pascal.taie.util.collection.Maps.newMap;
import static pascal.taie.util.collection.Sets.newSet;

abstract class AbstractCFG<N> implements CFG<N> {

    protected final IR ir;

    protected N entry;

    protected N exit;

    protected final Set<N> nodes = newSet();

    private final Map<N, Set<Edge<N>>> inEdges = newMap();

    private final Map<N, Set<Edge<N>>> outEdges = newMap();

    AbstractCFG(IR ir) {
        this.ir = ir;
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
            addToMapSet(inEdges, edge.getTarget(), edge);
            addToMapSet(outEdges, edge.getSource(), edge);
        }
    }

    /**
     * @return if the CFG already contains an existing edge with same
     * kind, source, and target of the given edge, return the existing edge,
     * otherwise, return null.
     */
    private @Nullable Edge<N> getExistingEdge(Edge<N> edge) {
        for (Edge<N> outEdge : outEdges.getOrDefault(
                edge.getSource(), Set.of())) {
            if (outEdge.getTarget().equals(edge.getTarget()) &&
                    outEdge.getKind() == edge.getKind()) {
                return outEdge;
            }
        }
        return null;
    }

    @Override
    public Stream<Edge<N>> inEdgesOf(N node) {
        return inEdges.getOrDefault(node, Set.of()).stream();
    }

    @Override
    public Stream<Edge<N>> outEdgesOf(N node) {
        return outEdges.getOrDefault(node, Set.of()).stream();
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
    public Stream<N> predsOf(N node) {
        return inEdgesOf(node).map(Edge::getSource);
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
