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

package pascal.taie.analysis.pta.cs;

import pascal.taie.analysis.pta.core.cs.element.Pointer;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Sets;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Represents pointer flow graph in context-sensitive pointer analysis.
 */
class PointerFlowGraph {

    /**
     * Map from a pointer (node) to its successors in PFG.
     */
    private final Map<Pointer, Set<Pointer>> successors = Maps.newMap();

    /**
     * Adds an edge (source -> target) to this PFG.
     *
     * @return true if this PFG changed as a result of the call,
     * otherwise false.
     */
    boolean addEdge(Pointer source, Pointer target) {
        return successors.computeIfAbsent(source, p -> Sets.newHybridSet())
                .add(target);
    }

    /**
     * @return successors of given pointer in the PFG.
     */
    Stream<Pointer> succsOf(Pointer pointer) {
        return successors.getOrDefault(pointer, Set.of()).stream();
    }
}
