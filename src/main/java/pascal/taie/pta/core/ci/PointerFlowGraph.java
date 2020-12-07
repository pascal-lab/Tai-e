/*
 * Panda - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Panda is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.taie.pta.core.ci;

import pascal.taie.pta.element.Field;
import pascal.taie.pta.element.Obj;
import pascal.taie.pta.element.Variable;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class PointerFlowGraph {

    /**
     * Set of all pointer in this PFG.
     */
    private final Set<Pointer> pointers = new HashSet<>();

    /**
     * Map from Variable to Var node.
     */
    private final Map<Variable, Var> vars = new HashMap<>();

    /**
     * Map from (Obj, Field) to InstanceField node.
     */
    private final Map<Obj, Map<Field, InstanceField>> instanceFields = new HashMap<>();

    /**
     * Map from a pointer (node) to its successors in PFG.
     */
    private final Map<Pointer, Set<Pointer>> successors = new HashMap<>();

    /**
     * Returns all pointers in this PFG.
     */
    Set<Pointer> getPointers() {
        return pointers;
    }

    /**
     * Returns the corresponding Var node for the given variable.
     */
    Var getVar(Variable variable) {
        return vars.computeIfAbsent(variable, v -> {
            Var var = new Var(v);
            pointers.add(var);
            return var;
        });
    }

    /**
     * Returns the corresponding instance field node
     * for the given object and field.
     */
    InstanceField getInstanceField(Obj base, Field field) {
        return instanceFields.computeIfAbsent(base, o -> new HashMap<>())
                .computeIfAbsent(field, f -> {
                    InstanceField instField = new InstanceField(base, f);
                    pointers.add(instField);
                    return instField;
                });
    }

    /**
     * Adds an edge from -> to to this PFG.
     * If the edge (from -> to) is already in this PFG, then returns false,
     * otherwise returns true.
     */
    boolean addEdge(Pointer from, Pointer to) {
        return successors.computeIfAbsent(from, p -> new HashSet<>())
                .add(to);
    }

    Set<Pointer> getSuccessorsOf(Pointer pointer) {
        return successors.getOrDefault(pointer, Collections.emptySet());
    }
}
