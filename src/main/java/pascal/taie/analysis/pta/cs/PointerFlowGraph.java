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
import pascal.taie.util.collection.MultiMap;

import java.util.Set;

/**
 * Represents pointer flow graph in context-sensitive pointer analysis.
 */
class PointerFlowGraph {

    /**
     * Map from a pointer (node) to its successors in PFG.
     */
    private final MultiMap<Pointer, Pointer> successors = Maps.newMultiMap();

    /**
     * Adds an edge (source -> target) to this PFG.
     *
     * @return true if this PFG changed as a result of the call,
     * otherwise false.
     */
    boolean addEdge(Pointer source, Pointer target) {
        return successors.put(source, target);
    }

    /**
     * @return successors of given pointer in the PFG.
     */
    Set<Pointer> getSuccsOf(Pointer pointer) {
        return successors.get(pointer);
    }
}
