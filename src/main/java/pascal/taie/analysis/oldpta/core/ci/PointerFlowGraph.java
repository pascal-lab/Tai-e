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

package pascal.taie.analysis.oldpta.core.ci;

import pascal.taie.analysis.oldpta.ir.Obj;
import pascal.taie.analysis.oldpta.ir.Variable;
import pascal.taie.language.classes.JField;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static pascal.taie.util.collection.CollectionUtils.newHybridSet;
import static pascal.taie.util.collection.CollectionUtils.newMap;
import static pascal.taie.util.collection.CollectionUtils.newSet;

class PointerFlowGraph {

    /**
     * Set of all pointer in this PFG.
     */
    private final Set<Pointer> pointers = newSet();

    /**
     * Map from Variable to Var node.
     */
    private final Map<Variable, Var> vars = newMap();

    /**
     * Map from (Obj, Field) to InstanceField node.
     */
    private final Map<Obj, Map<JField, InstanceField>> instanceFields = newMap();

    /**
     * Map from a pointer (node) to its successors in PFG.
     */
    private final Map<Pointer, Set<Pointer>> successors = newMap();

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
    InstanceField getInstanceField(Obj base, JField field) {
        return instanceFields.computeIfAbsent(base, o -> newMap())
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
        return successors.computeIfAbsent(from, p -> newHybridSet())
                .add(to);
    }

    Set<Pointer> getSuccessorsOf(Pointer pointer) {
        return successors.getOrDefault(pointer, Collections.emptySet());
    }
}
