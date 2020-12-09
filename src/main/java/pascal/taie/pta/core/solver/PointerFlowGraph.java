/*
 * Tai-e: A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Tai-e is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.taie.pta.core.solver;

import pascal.taie.pta.core.cs.Pointer;
import pascal.taie.pta.element.Type;

import java.util.HashSet;
import java.util.Set;

public class PointerFlowGraph {

    private final Set<Pointer> pointers = new HashSet<>();

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
