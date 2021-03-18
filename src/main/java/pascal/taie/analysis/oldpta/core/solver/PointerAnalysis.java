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

import pascal.taie.analysis.graph.callgraph.CallGraph;
import pascal.taie.analysis.oldpta.core.context.Context;
import pascal.taie.analysis.oldpta.core.context.ContextSelector;
import pascal.taie.analysis.oldpta.core.cs.ArrayIndex;
import pascal.taie.analysis.oldpta.core.cs.CSCallSite;
import pascal.taie.analysis.oldpta.core.cs.CSManager;
import pascal.taie.analysis.oldpta.core.cs.CSMethod;
import pascal.taie.analysis.oldpta.core.cs.CSVariable;
import pascal.taie.analysis.oldpta.core.cs.InstanceField;
import pascal.taie.analysis.oldpta.core.cs.StaticField;
import pascal.taie.analysis.oldpta.env.Environment;
import pascal.taie.analysis.oldpta.ir.Obj;
import pascal.taie.analysis.oldpta.ir.Variable;
import pascal.taie.analysis.oldpta.set.PointsToSet;
import pascal.taie.language.classes.ClassHierarchy;

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
