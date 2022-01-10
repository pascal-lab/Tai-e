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

package pascal.taie.analysis.pta.core.solver;

import pascal.taie.analysis.pta.core.cs.element.Pointer;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.Sets;
import pascal.taie.util.graph.Edge;
import pascal.taie.util.graph.Graph;

import java.util.Set;
import java.util.stream.Stream;

/**
 * Represents pointer flow graph in context-sensitive pointer analysis.
 */
public class PointerFlowGraph implements Graph<Pointer> {

    private final Set<Pointer> pointers = Sets.newSet();

    public boolean addEdge(Pointer source, Pointer target,
                           PointerFlowEdge.Kind kind) {
        return addEdge(source, target, null, kind);
    }

    public boolean addEdge(Pointer source, Pointer target, Type type,
                           PointerFlowEdge.Kind kind) {
        if (source.addOutEdge(new PointerFlowEdge(kind, source, target, type))) {
            pointers.add(source);
            pointers.add(target);
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
    public Stream<PointerFlowEdge> outEdgesOf(Pointer pointer) {
        return pointer.outEdges();
    }

    @Override
    public int getOutDegreeOf(Pointer node) {
        return node.getOutDegree();
    }

    public Stream<Pointer> pointers() {
        return pointers.stream();
    }

    @Override
    public boolean hasNode(Pointer node) {
        return pointers.contains(node);
    }

    @Override
    public boolean hasEdge(Pointer source, Pointer target) {
        return succsOf(source).anyMatch(target::equals);
    }

    @Override
    public Set<Pointer> getPredsOf(Pointer node) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Stream<Pointer> succsOf(Pointer node) {
        return node.outEdges().map(PointerFlowEdge::getTarget);
    }

    @Override
    public Stream<Pointer> nodes() {
        return pointers();
    }

    @Override
    public int getNumberOfNodes() {
        return pointers.size();
    }
}
