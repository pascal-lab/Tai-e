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

import pascal.taie.callgraph.CallGraph;
import pascal.taie.ir.exp.Var;
import pascal.taie.java.ClassHierarchy;
import pascal.taie.java.classes.JField;
import pascal.taie.pta.core.context.Context;
import pascal.taie.pta.core.context.ContextSelector;
import pascal.taie.pta.core.cs.ArrayIndex;
import pascal.taie.pta.core.cs.CSCallSite;
import pascal.taie.pta.core.cs.CSManager;
import pascal.taie.pta.core.cs.CSMethod;
import pascal.taie.pta.core.cs.CSVar;
import pascal.taie.pta.core.cs.InstanceField;
import pascal.taie.pta.core.cs.StaticField;
import pascal.taie.pta.core.heap.HeapModel;
import pascal.taie.pta.core.heap.Obj;
import pascal.taie.pta.set.PointsToSet;

import java.util.stream.Stream;

public interface PointerAnalysis {

    ClassHierarchy getHierarchy();

    HeapModel getHeapModel();

    CSManager getCSManager();

    ContextSelector getContextSelector();

    CallGraph<CSCallSite, CSMethod> getCallGraph();

    void analyze();

    /**
     * Add a context-sensitive variable points-to relation.
     * @param context context of the method which contains the variable
     * @param var the variable
     * @param heapContext heap context for the object
     * @param obj the object to be added
     */
    void addVarPointsTo(Context context, Var var,
                        Context heapContext, Obj obj);

    void addVarPointsTo(Context context, Var var, PointsToSet pts);

    /**
     * Add a context-sensitive array index points-to relation.
     * @param arrayContext heap context of the array object
     * @param array the array object
     * @param heapContext heap context for the element
     * @param obj the element to be stored into the array
     */
    void addArrayPointsTo(Context arrayContext, Obj array,
                          Context heapContext, Obj obj);

    /**
     * Add static field points-to relations.
     * @param field the static field
     * @param pts the objects to be added to the points-to set of the field.
     */
    void addStaticFieldPointsTo(JField field, PointsToSet pts);

    /**
     * @return all variables in the (reachable) program.
     */
    Stream<CSVar> getVars();

    /**
     * @return all instance fields in the (reachable) program.
     */
    Stream<InstanceField> getInstanceFields();

    /**
     * @return all array indexes in the (reachable) program.
     */
    Stream<ArrayIndex> getArrayIndexes();

    /**
     * @return all static fields in the (reachable) program.
     */
    Stream<StaticField> getStaticFields();
}
