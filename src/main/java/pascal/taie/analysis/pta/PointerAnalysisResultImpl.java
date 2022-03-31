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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.analysis.graph.callgraph.CallGraph;
import pascal.taie.analysis.graph.callgraph.DefaultCallGraph;
import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.pta.core.cs.element.ArrayIndex;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSManager;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.cs.element.InstanceField;
import pascal.taie.analysis.pta.core.cs.element.Pointer;
import pascal.taie.analysis.pta.core.cs.element.StaticField;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.AbstractResultHolder;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Pair;
import pascal.taie.util.collection.Sets;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PointerAnalysisResultImpl extends AbstractResultHolder
        implements PointerAnalysisResult {

    private static final Logger logger = LogManager.getLogger(PointerAnalysisResultImpl.class);

    private final CSManager csManager;

    private final Map<Var, Set<Obj>> varPointsTo = Maps.newMap();

    /**
     * Points-to sets of field expressions, e.g., v.f.
     */
    private final Map<Pair<Var, JField>, Set<Obj>> fieldPointsTo = Maps.newMap();

    /**
     * Context-sensitive call graph.
     */
    private final CallGraph<CSCallSite, CSMethod> csCallGraph;

    /**
     * Call graph (context projected out).
     */
    private CallGraph<Invoke, JMethod> callGraph;

    public PointerAnalysisResultImpl(CSManager csManager,
                                     CallGraph<CSCallSite, CSMethod> csCallGraph) {
        this.csManager = csManager;
        this.csCallGraph = csCallGraph;
    }

    @Override
    public Collection<CSVar> getCSVars() {
        return csManager.getCSVars();
    }

    @Override
    public Collection<Var> getVars() {
        return csManager.getVars();
    }

    @Override
    public Collection<InstanceField> getInstanceFields() {
        return csManager.getInstanceFields();
    }

    @Override
    public Collection<ArrayIndex> getArrayIndexes() {
        return csManager.getArrayIndexes();
    }

    @Override
    public Collection<StaticField> getStaticFields() {
        return csManager.getStaticFields();
    }

    @Override
    public Collection<CSObj> getCSObjects() {
        return csManager.getObjects();
    }

    @Override
    public Collection<Obj> getObjects() {
        return removeContexts(getCSObjects().stream());
    }

    @Override
    public Set<Obj> getPointsToSet(Var var) {
        return varPointsTo.computeIfAbsent(var, v ->
                removeContexts(csManager.getCSVarsOf(var)
                        .stream()
                        .flatMap(Pointer::objects)));
    }

    @Override
    public Set<Obj> getPointsToSet(Var base, JField field) {
        if (field.isStatic()) {
            logger.warn("{} is not instance field", field);
        }
        return fieldPointsTo.computeIfAbsent(new Pair<>(base, field), p -> {
            Set<Obj> pts = Sets.newHybridSet();
            csManager.getCSVarsOf(base)
                    .stream()
                    .flatMap(Pointer::objects)
                    .forEach(o -> {
                        InstanceField ifield = csManager.getInstanceField(o, field);
                        ifield.objects()
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
        return removeContexts(csManager.getStaticField(field).objects());
    }

    /**
     * Removes contexts of a context-sensitive points-to set and
     * returns a new resulting set.
     */
    private static Set<Obj> removeContexts(Stream<CSObj> objects) {
        return objects.map(CSObj::getObject)
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

    /**
     * Removes contexts in a context-sensitive call graph and
     * returns a new resulting call graph.
     */
    private static CallGraph<Invoke, JMethod> removeContexts(
            CallGraph<CSCallSite, CSMethod> csCallGraph) {
        DefaultCallGraph callGraph = new DefaultCallGraph();
        csCallGraph.entryMethods()
                .map(CSMethod::getMethod)
                .forEach(callGraph::addEntryMethod);
        csCallGraph.reachableMethods()
                .map(CSMethod::getMethod)
                .forEach(callGraph::addReachableMethod);
        csCallGraph.edges().forEach(edge -> {
            Invoke callSite = edge.getCallSite().getCallSite();
            JMethod callee = edge.getCallee().getMethod();
            callGraph.addEdge(new Edge<>(edge.getKind(),
                    callSite, callee));
        });
        return callGraph;
    }
}
