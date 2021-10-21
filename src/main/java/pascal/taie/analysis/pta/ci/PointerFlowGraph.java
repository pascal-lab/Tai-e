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

import static pascal.taie.util.collection.Maps.newHybridMap;
import static pascal.taie.util.collection.Maps.newMap;
import static pascal.taie.util.collection.Sets.newHybridSet;
import static pascal.taie.util.collection.Sets.newSet;

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
     * Map from JField to StaticField node.
     */
    private final Map<JField, StaticField> staticFields = newMap();

    /**
     * Map from (Obj, Field) to InstanceField node.
     */
    private final Map<Obj, Map<JField, InstanceField>> instanceFields = newMap();

    /**
     * Map from Obj (array) to ArrayIndex node.
     */
    private final Map<Obj, ArrayIndex> arrayIndexes = newMap();

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
     * @return the corresponding Var node for the given variable.
     */
    VarPtr getVarPtr(Var var) {
        return varPtrs.computeIfAbsent(var, v -> {
            VarPtr varPtr = new VarPtr(v);
            pointers.add(varPtr);
            return varPtr;
        });
    }

    /**
     * @return the corresponding StaticField node for the given field.
     * The field should be a static field.
     */
    StaticField getStaticField(JField field) {
        return staticFields.computeIfAbsent(field, f -> {
            StaticField staticField = new StaticField(f);
            pointers.add(staticField);
            return staticField;
        });
    }

    /**
     * @return the corresponding InstanceField node for the given object and field.
     * The field should be an instance field.
     */
    InstanceField getInstanceField(Obj base, JField field) {
        return instanceFields.computeIfAbsent(base, o -> newHybridMap())
                .computeIfAbsent(field, f -> {
                    InstanceField instanceField = new InstanceField(base, f);
                    pointers.add(instanceField);
                    return instanceField;
                });
    }

    /**
     * @return the corresponding ArrayIndex node for the given array object.
     */
    ArrayIndex getArrayIndex(Obj array) {
        return arrayIndexes.computeIfAbsent(array, a -> {
            ArrayIndex arrayIndex = new ArrayIndex(a);
            pointers.add(arrayIndex);
            return arrayIndex;
        });
    }

    /**
     * Adds an edge (from -> to) to this PFG.
     * If the edge (from -> to) has already been added in this PFG,
     * then returns false, otherwise returns true.
     */
    boolean addEdge(Pointer from, Pointer to) {
        return successors.computeIfAbsent(from, p -> newHybridSet())
                .add(to);
    }

    /**
     * @return successors of given pointer in the PFG.
     */
    Stream<Pointer> succsOf(Pointer pointer) {
        return successors.getOrDefault(pointer, Set.of()).stream();
    }
}
