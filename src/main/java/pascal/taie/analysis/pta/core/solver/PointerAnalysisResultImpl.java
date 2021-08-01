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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.analysis.graph.callgraph.CallGraph;
import pascal.taie.analysis.graph.callgraph.DefaultCallGraph;
import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.core.cs.element.ArrayIndex;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSManager;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.cs.element.InstanceField;
import pascal.taie.analysis.pta.core.cs.element.StaticField;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.plugin.exception.PTAThrowResult;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.MapUtils;
import pascal.taie.util.collection.Pair;
import pascal.taie.util.collection.SetUtils;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class PointerAnalysisResultImpl implements PointerAnalysisResult {

    private static final Logger logger = LogManager.getLogger(PointerAnalysisResultImpl.class);

    private final CSManager csManager;

    private final Map<Var, Set<Obj>> varPointsTo = MapUtils.newMap();

    /**
     * Points-to sets of field expressions, e.g., v.f.
     */
    private final Map<Pair<Var, JField>, Set<Obj>> fieldPointsTo = MapUtils.newMap();

    /**
     * Context-sensitive call graph.
     */
    private final CallGraph<CSCallSite, CSMethod> csCallGraph;

    /**
     * PTA-based throw analysis result.
     */
    private final PTAThrowResult throwResult;

    /**
     * Call graph (context projected out).
     */
    private CallGraph<Invoke, JMethod> callGraph;

    PointerAnalysisResultImpl(CSManager csManager,
                              CallGraph<CSCallSite, CSMethod> csCallGraph,
                              PTAThrowResult throwResult) {
        this.csManager = csManager;
        this.csCallGraph = csCallGraph;
        this.throwResult = throwResult;
    }

    @Override
    public Stream<CSVar> csVars() {
        return csManager.csVars();
    }

    @Override
    public Stream<Var> vars() {
        return csVars().map(CSVar::getVar).distinct();
    }

    @Override
    public Stream<InstanceField> instanceFields() {
        return csManager.instanceFields();
    }

    @Override
    public Stream<ArrayIndex> arrayIndexes() {
        return csManager.arrayIndexes();
    }

    @Override
    public Stream<StaticField> staticFields() {
        return csManager.staticFields();
    }

    @Override
    public Stream<CSObj> csObjects() {
        return csManager.objects();
    }

    @Override
    public Stream<Obj> objects() {
        return csObjects().map(CSObj::getObject).distinct();
    }

    @Override
    public Set<CSObj> getPointsToSet(CSVar var) {
        return var.getPointsToSet().getObjects();
    }

    @Override
    public Set<Obj> getPointsToSet(Var var) {
        return varPointsTo.computeIfAbsent(var, v ->
            csManager.csVarsOf(var)
                    .flatMap(csVar -> csVar.getPointsToSet().objects())
                    .map(CSObj::getObject)
                    .collect(Collectors.toUnmodifiableSet()));
    }

    @Override
    public Set<Obj> getPointsToSet(Var base, JField field) {
        if (field.isStatic()) {
            logger.warn("{} is not instance field", field);
        }
        return fieldPointsTo.computeIfAbsent(new Pair<>(base, field), p -> {
            Set<Obj> pts = SetUtils.newHybridSet();
            csManager.csVarsOf(base)
                    .flatMap(csVar -> csVar.getPointsToSet().objects())
                    .forEach(o -> {
                        InstanceField ifield = csManager.getInstanceField(o, field);
                        ifield.getPointsToSet().objects()
                                .map(CSObj::getObject)
                                .forEach(pts::add);
                    });
            return Collections.unmodifiableSet(pts);
        });
    }

    @Override
    public Set<Obj> getPointsToSet(JField field) {
        if (!field.isStatic()) {
            logger.warn("{} is not static field", field);
        }
        return removeContexts(
                csManager.getStaticField(field).getPointsToSet());
    }

    /**
     * Removes contexts of a context-sensitive points-to set and
     * returns a new resulting set.
     */
    private static Set<Obj> removeContexts(PointsToSet pts) {
        return pts.objects().map(CSObj::getObject)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public CallGraph<CSCallSite, CSMethod> getCSCallGraph() {
        return csCallGraph;
    }

    @Override
    public CallGraph<Invoke, JMethod> getCallGraph() {
        if (callGraph == null) {
            callGraph = removeContexts(csCallGraph);
        }
        return callGraph;
    }

    @Override
    public PTAThrowResult getThrowResult(){
        return this.throwResult;
    }

    /**
     * Removes contexts in a context-sensitive call graph and
     * returns a new resulting call graph.
     */
    private static CallGraph<Invoke, JMethod> removeContexts(
            CallGraph<CSCallSite, CSMethod> csCallGraph) {
        DefaultCallGraph callGraph = new DefaultCallGraph();
        csCallGraph.entryMethods().map(CSMethod::getMethod).
                forEach(callGraph::addEntryMethod);
        csCallGraph.reachableMethods()
                .map(CSMethod::getMethod)
                .forEach(callGraph::addNewMethod);
        csCallGraph.edges().forEach(edge -> {
            Invoke callSite = edge.getCallSite().getCallSite();
            JMethod callee = edge.getCallee().getMethod();
            callGraph.addEdge(new Edge<>(edge.getKind(),
                    callSite, callee));
        });
        return callGraph;
    }
}
