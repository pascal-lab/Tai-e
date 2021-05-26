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

public interface PointerAnalysisResult {

    /**
     * @return all variables in the (reachable) program.
     */
    Stream<CSVar> csVars();

    Stream<Var> vars();

    /**
     * @return all instance fields in the (reachable) program.
     */
    Stream<InstanceField> instanceFields();

    /**
     * @return all array indexes in the (reachable) program.
     */
    Stream<ArrayIndex> arrayIndexes();

    /**
     * @return all static fields in the (reachable) program.
     */
    Stream<StaticField> staticFields();

    Stream<CSObj> csObjects();

    Stream<Obj> objects();

    Set<CSObj> getPointsToSet(CSVar var);

    Set<Obj> getPointsToSet(Var var);

    Set<Obj> getPointsToSet(Var base, JField field);

    Set<Obj> getPointsToSet(JField field);

    CallGraph<CSCallSite, CSMethod> getCSCallGraph();

    CallGraph<Invoke, JMethod> getCallGraph();
}
