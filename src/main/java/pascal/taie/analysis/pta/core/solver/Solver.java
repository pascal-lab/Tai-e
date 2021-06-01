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

package pascal.taie.analysis.pta.core.solver;

import pascal.taie.analysis.graph.callgraph.CallGraph;
import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSManager;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.cs.element.Pointer;
import pascal.taie.analysis.pta.core.cs.selector.ContextSelector;
import pascal.taie.analysis.pta.core.heap.HeapModel;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.config.AnalysisOptions;
import pascal.taie.ir.exp.Var;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JField;
import pascal.taie.language.type.TypeManager;

public interface Solver {

    AnalysisOptions getOptions();

    ClassHierarchy getHierarchy();

    TypeManager getTypeManager();

    HeapModel getHeapModel();

    CSManager getCSManager();

    ContextSelector getContextSelector();

    CallGraph<CSCallSite, CSMethod> getCallGraph();

    void solve();

    PointsToSet getPointsToSetOf(Pointer pointer);

    /**
     * Adds a context-sensitive variable points-to relation.
     * @param context context of the method which contains the variable
     * @param var the variable
     * @param heapContext heap context for the object
     * @param obj the object to be added
     */
    void addVarPointsTo(Context context, Var var,
                        Context heapContext, Obj obj);

    void addVarPointsTo(Context context, Var var, CSObj csObj);

    void addVarPointsTo(Context context, Var var, PointsToSet pts);

    /**
     * Adds a context-sensitive array index points-to relation.
     * @param arrayContext heap context of the array object
     * @param array the array object
     * @param heapContext heap context for the element
     * @param obj the element to be stored into the array
     */
    void addArrayPointsTo(Context arrayContext, Obj array,
                          Context heapContext, Obj obj);

    /**
     * Adds static field points-to relations.
     * @param field the static field
     * @param pts the objects to be added to the points-to set of the field.
     */
    void addStaticFieldPointsTo(JField field, PointsToSet pts);

    /**
     * Adds an edge "from -> to" to the PFG.
     */
    void addPFGEdge(Pointer from, Pointer to, PointerFlowEdge.Kind kind);

    /**
     * Adds a call edge.
     * @param edge the added edge.
     */
    void addCallEdge(Edge<CSCallSite, CSMethod> edge);

    /**
     * Adds a context-sensitive method.
     * @param csMethod the added contxt-sensitive method.
     */
    void addCSMethod(CSMethod csMethod);

    PointerAnalysisResult getResult();
}
