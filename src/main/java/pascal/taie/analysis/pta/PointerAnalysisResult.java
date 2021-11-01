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

package pascal.taie.analysis.pta;

import pascal.taie.analysis.ResultHolder;
import pascal.taie.analysis.graph.callgraph.CallGraph;
import pascal.taie.analysis.pta.core.cs.element.ArrayIndex;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.cs.element.InstanceField;
import pascal.taie.analysis.pta.core.cs.element.StaticField;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;

import java.util.Set;
import java.util.stream.Stream;

/**
 * Represents results of pointer analysis.
 */
public interface PointerAnalysisResult extends ResultHolder {

    /**
     * @return all reachable context-sensitive variables in the program.
     */
    Stream<CSVar> csVars();

    /**
     * @return all reachable variables in the program.
     */
    Stream<Var> vars();

    /**
     * @return all reachable instance fields in the program.
     */
    Stream<InstanceField> instanceFields();

    /**
     * @return all reachable array indexes in the program.
     */
    Stream<ArrayIndex> arrayIndexes();

    /**
     * @return all reachable static fields in the program.
     */
    Stream<StaticField> staticFields();

    /**
     * @return all reachable context-sensitive objects in the program.
     */
    Stream<CSObj> csObjects();

    /**
     * @return all reachable objects in the program.
     */
    Stream<Obj> objects();

    /**
     * @return context-sensitive points-to set of context-sensitive variable var.
     */
    Set<CSObj> getPointsToSet(CSVar var);

    /**
     * @return set of Obj pointed to by var.
     */
    Set<Obj> getPointsToSet(Var var);

    /**
     * @return set of Obj pointed to by base.field.
     */
    Set<Obj> getPointsToSet(Var base, JField field);

    /**
     * @return points-to set of given field. The field is supposed to be static.
     */
    Set<Obj> getPointsToSet(JField field);

    /**
     * @return the resulting context-sensitive call graph.
     */
    CallGraph<CSCallSite, CSMethod> getCSCallGraph();

    /**
     * @return the resulting call graph (without contexts).
     */
    CallGraph<Invoke, JMethod> getCallGraph();
}
