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

package pascal.taie.analysis.oldpta.core.solver;

import pascal.taie.language.types.Type;
import pascal.taie.analysis.oldpta.core.cs.Pointer;

import java.util.Set;

import static pascal.taie.util.collection.CollectionUtils.newSet;

public class PointerFlowGraph {

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

    public Set<PointerFlowEdge> getOutEdgesOf(Pointer pointer) {
        return pointer.getOutEdges();
    }

    public Set<Pointer> getPointers() {
        return pointers;
    }
}
