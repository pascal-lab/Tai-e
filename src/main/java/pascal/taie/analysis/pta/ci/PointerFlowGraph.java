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

package pascal.taie.analysis.pta.ci;

import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.ir.exp.Var;
import pascal.taie.language.classes.JField;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static pascal.taie.util.collection.MapUtils.newHybridMap;
import static pascal.taie.util.collection.MapUtils.newMap;
import static pascal.taie.util.collection.SetUtils.newHybridSet;
import static pascal.taie.util.collection.SetUtils.newSet;

class PointerFlowGraph {

    /**
     * Set of all pointer in this PFG.
     */
    private final Set<Pointer> pointers = newSet();

    /**
     * Map from Variable to Var node.
     */
    private final Map<Var, VarPtr> varPtrs = newMap();

    /**
     * Map from (Obj, Field) to InstanceField node.
     */
    private final Map<Obj, Map<JField, InstanceFieldPtr>> fieldPtrs = newMap();

    /**
     * Map from a pointer (node) to its successors in PFG.
     */
    private final Map<Pointer, Set<Pointer>> successors = newMap();

    /**
     * Returns all pointers in this PFG.
     */
    Stream<Pointer> pointers() {
        return pointers.stream();
    }

    /**
     * Returns the corresponding Var node for the given variable.
     */
    VarPtr getVarPtr(Var var) {
        return varPtrs.computeIfAbsent(var, v -> {
            VarPtr varPtr = new VarPtr(v);
            pointers.add(varPtr);
            return varPtr;
        });
    }

    /**
     * Returns the corresponding instance field node
     * for the given object and field.
     */
    InstanceFieldPtr getInstanceFieldPtr(Obj base, JField field) {
        return fieldPtrs.computeIfAbsent(base, o -> newHybridMap())
                .computeIfAbsent(field, f -> {
                    InstanceFieldPtr fieldPtr = new InstanceFieldPtr(base, f);
                    pointers.add(fieldPtr);
                    return fieldPtr;
                });
    }

    /**
     * Adds an edge (from -> to) to this PFG.
     * If the edge (from -> to) is already in this PFG, then returns false,
     * otherwise returns true.
     */
    boolean addEdge(Pointer from, Pointer to) {
        return successors.computeIfAbsent(from, p -> newHybridSet())
                .add(to);
    }

    Stream<Pointer> succsOf(Pointer pointer) {
        return successors.getOrDefault(pointer, Set.of()).stream();
    }
}
