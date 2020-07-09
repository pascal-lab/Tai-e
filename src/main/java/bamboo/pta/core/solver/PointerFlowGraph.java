/*
 * Bamboo - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Bamboo is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package bamboo.pta.core.solver;

import bamboo.pta.core.cs.Pointer;
import bamboo.pta.element.Type;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class PointerFlowGraph {

    private final Set<Pointer> pointers = new HashSet<>();

    private final Map<Pointer, Set<PointerFlowEdge>> edges = new HashMap<>();

    private final Map<Pointer, Set<Pointer>> successors = new HashMap<>();

    public boolean addEdge(Pointer from, Pointer to, PointerFlowEdge.Kind kind) {
        return addEdge(from, to, null, kind);
    }

    public boolean addEdge(Pointer from, Pointer to, Type type, PointerFlowEdge.Kind kind) {
        if (!successors.computeIfAbsent(from, k -> new HashSet<>()).contains(to)) {
            successors.get(from).add(to);
            edges.computeIfAbsent(from, k -> new HashSet<>())
                    .add(new PointerFlowEdge(kind, from, to, type));
            pointers.add(from);
            pointers.add(to);
            return true;
        } else {
            return false;
        }
    }

    public Set<PointerFlowEdge> getOutEdgesOf(Pointer pointer) {
        return edges.getOrDefault(pointer, Collections.emptySet());
    }

    public Set<Pointer> getPointers() {
        return pointers;
    }

    public Iterator<PointerFlowEdge> getEdges() {
        return edges.values()
                .stream()
                .flatMap(Set::stream)
                .iterator();
    }
}
