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

package panda.pta.core.solver;

import panda.callgraph.CallGraph;
import panda.pta.core.ProgramManager;
import panda.pta.core.context.Context;
import panda.pta.core.context.ContextSelector;
import panda.pta.core.cs.ArrayIndex;
import panda.pta.core.cs.CSCallSite;
import panda.pta.core.cs.CSManager;
import panda.pta.core.cs.CSMethod;
import panda.pta.core.cs.CSVariable;
import panda.pta.core.cs.InstanceField;
import panda.pta.core.cs.StaticField;
import panda.pta.element.Obj;
import panda.pta.element.Variable;
import panda.pta.set.PointsToSet;

import java.util.stream.Stream;

public interface PointerAnalysis {

    ProgramManager getProgramManager();

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
