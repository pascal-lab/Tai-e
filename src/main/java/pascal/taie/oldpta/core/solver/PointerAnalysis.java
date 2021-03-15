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

package pascal.taie.oldpta.core.solver;

import pascal.taie.callgraph.CallGraph;
import pascal.taie.java.ClassHierarchy;
import pascal.taie.oldpta.core.context.Context;
import pascal.taie.oldpta.core.context.ContextSelector;
import pascal.taie.oldpta.core.cs.ArrayIndex;
import pascal.taie.oldpta.core.cs.CSCallSite;
import pascal.taie.oldpta.core.cs.CSManager;
import pascal.taie.oldpta.core.cs.CSMethod;
import pascal.taie.oldpta.core.cs.CSVariable;
import pascal.taie.oldpta.core.cs.InstanceField;
import pascal.taie.oldpta.core.cs.StaticField;
import pascal.taie.oldpta.env.Environment;
import pascal.taie.oldpta.ir.Obj;
import pascal.taie.oldpta.ir.Variable;
import pascal.taie.oldpta.set.PointsToSet;

import java.util.stream.Stream;

public interface PointerAnalysis {

    ClassHierarchy getHierarchy();

    Environment getEnvironment();

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
    void addPointsTo(Context context, Variable var,
                     Context heapContext, Obj obj);

    void addPointsTo(Context context, Variable var, PointsToSet pts);

    /**
     * Add a context-sensitive array index points-to relation.
     * @param arrayContext heap context of the array object
     * @param array the array object
     * @param heapContext heap context for the element
     * @param obj the element to be stored into the array
     */
    void addPointsTo(Context arrayContext, Obj array,
                     Context heapContext, Obj obj);

    /**
     * @return all variables in the (reachable) program.
     */
    Stream<CSVariable> getVariables();

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
