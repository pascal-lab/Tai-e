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

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static pascal.taie.util.collection.CollectionUtils.newHybridSet;
import static pascal.taie.util.collection.CollectionUtils.newMap;
import static pascal.taie.util.collection.CollectionUtils.newSet;

abstract class AbstractCFG<N> implements CFG<N> {

    protected final IR ir;

    protected N entry;

    protected N exit;

    protected final Set<N> nodes = newSet();

    protected final Map<N, Set<Edge<N>>> inEdges = newMap();

    protected final Map<N, Set<Edge<N>>> outEdges = newMap();

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

    void addEdge(Edge<N> edge) {
        nodes.add(edge.getSource());
        nodes.add(edge.getTarget());
        inEdges.computeIfAbsent(edge.getTarget(), n -> newHybridSet())
                .add(edge);
        outEdges.computeIfAbsent(edge.getSource(), n -> newHybridSet())
                .add(edge);
    }

    @Override
    public Stream<Edge<N>> inEdgesOf(N node) {
        return inEdges.getOrDefault(node, Collections.emptySet()).stream();
    }

    @Override
    public Stream<Edge<N>> outEdgesOf(N node) {
        return outEdges.getOrDefault(node, Collections.emptySet()).stream();
    }

    @Override
    public Stream<N> predsOf(N node, Edge.Kind kind) {
        return inEdgesOf(node, kind).map(Edge::getSource);
    }

    @Override
    public Stream<N> succsOf(N node, Edge.Kind kind) {
        return outEdgesOf(node, kind).map(Edge::getTarget);
    }

    @Override
    public Stream<Edge<N>> inEdgesOf(N node, Edge.Kind kind) {
        return inEdgesOf(node).filter(e -> e.getKind() == kind);
    }

    @Override
    public Stream<Edge<N>> outEdgesOf(N node, Edge.Kind kind) {
        return outEdgesOf(node).filter(e -> e.getKind() == kind);
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
