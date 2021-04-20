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
import pascal.taie.util.graph.Graph;

import java.util.Set;
import java.util.stream.Stream;

import static pascal.taie.util.collection.CollectionUtils.newSet;

public class PointerFlowGraph implements Graph<Pointer> {

    private final Set<Pointer> pointers = newSet();

    public boolean addEdge(Pointer from, Pointer to,
                           PointerFlowEdge.Kind kind) {
        return addEdge(from, to, null, kind);
    }

    public boolean addEdge(Pointer from, Pointer to, Type type,
                           PointerFlowEdge.Kind kind) {
        if (from.addOutEdge(new PointerFlowEdge(kind, from, to, type))) {
            pointers.add(from);
            pointers.add(to);
            return true;
        } else {
            return false;
        }
    }

    public Stream<PointerFlowEdge> outEdgesOf(Pointer pointer) {
        return pointer.outEdges();
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
    public Stream<Pointer> predsOf(Pointer node) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Stream<Pointer> succsOf(Pointer node) {
        return node.outEdges().map(PointerFlowEdge::getTo);
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
