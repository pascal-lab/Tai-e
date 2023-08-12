package pascal.taie.analysis.pta.plugin.taint;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.analysis.graph.callgraph.CallGraph;
import pascal.taie.analysis.graph.flowgraph.*;
import pascal.taie.analysis.pta.core.cs.element.*;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.core.solver.PointerFlowEdge;
import pascal.taie.analysis.pta.core.solver.PointerFlowGraph;
import pascal.taie.analysis.pta.plugin.util.InvokeUtils;
import pascal.taie.analysis.pta.plugin.util.StrategyUtils;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Sets;
import pascal.taie.util.collection.Views;
import pascal.taie.util.graph.Graph;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TaintPointerFlowGraph implements Graph<Pointer> {

    private static final Logger logger = LogManager.getLogger(TaintPointerFlowGraph.class);

    private final MultiMap<Pointer, PointerFlowEdge> inEdges = Maps.newMultiMap(4096);

    private final MultiMap<Pointer, PointerFlowEdge> outEdges = Maps.newMultiMap(4096);

    private final Set<Pointer> taintedPointers = Sets.newSet();

    private final Set<Pointer> sourcePointers = Sets.newSet();

    private final Set<Pointer> sinkPointers = Sets.newSet();

    private final CallGraph<CSCallSite, CSMethod> callGraph;

    private final CSManager csManager;

    private final TaintManager taintManager;

    public TaintPointerFlowGraph(PointerFlowGraph pfg,
                                 CallGraph<CSCallSite, CSMethod> callGraph, CSManager csManager, TaintManager taintManager) {
        this.callGraph = callGraph;
        this.csManager = csManager;
        this.taintManager = taintManager;

        pfg.pointers()
                .filter(this::hasTaint)
                .peek(taintedPointers::add)
                .flatMap(pointer -> pointer.getOutEdges().stream())
                .filter(edge -> hasTaint(edge.target()))
                .forEach(edge -> {
                    addEdge(edge.kind(), edge.source(), edge.target());
                });

        StrategyUtils.getMethod2CSCallSites(callGraph).values().stream()
                .filter(csCallSite -> !csCallSite.getCallSite().isStatic())
                .forEach(this::addBase2ThisEdge);
    }

    private void addEdge(FlowKind kind, Pointer source, Pointer target) {
        PointerFlowEdge edge = new PointerFlowEdge(kind, source, target);
        outEdges.put(source, edge);
        inEdges.put(target, edge);
    }

    private void addBase2ThisEdge(CSCallSite csCallSite) {
        CSVar base = StrategyUtils.getCSVar(csManager, csCallSite, InvokeUtils.BASE);
        callGraph.getCalleesOf(csCallSite).forEach(callee -> {
            CSVar thisVar = csManager.getCSVar(callee.getContext(), callee.getMethod().getIR().getThis());
            Set<Type> thisObjTypes = thisVar.getObjects().stream()
                    .map(CSObj::getObject)
                    .map(Obj::getType)
                    .collect(Collectors.toSet());

            if (!Collections.disjoint(getTaintedTypes(base), thisObjTypes)) {
                addEdge(FlowKind.THIS_PASSING, base, thisVar);
            }
        });
    }

    private boolean hasTaint(Pointer pointer) {
        return pointer.getObjects()
                .stream()
                .map(CSObj::getObject)
                .anyMatch(taintManager::isTaint);
    }

    private Set<Type> getTaintedTypes(CSVar base) {
        if (base == null) {
            return Set.of();
        }

        Map<Boolean, List<CSObj>> partitioned = base.getObjects().stream()
                .collect(Collectors.partitioningBy(csObj -> taintManager.isTaint(csObj.getObject())));

        Set<CSObj> taints = Sets.newSet(partitioned.get(true));
        Set<CSObj> objs = Sets.newSet(partitioned.get(false));

        Set<CSObj> csObjs = taints.stream()
                .flatMap(taint -> objs.stream()
                        .filter(obj -> taint.getObject().getType().equals(obj.getObject().getType())))
                .collect(Collectors.toSet());

        return csObjs.stream()
                .map(CSObj::getObject)
                .map(Obj::getType)
                .collect(Collectors.toSet());
    }

    public Set<Pointer> getSourcePointers()
    {
        return null;
    }

    public Set<Pointer> getSinkPointers()
    {
        return null;
    }

    @Override
    public Set<Pointer> getPredsOf(Pointer pointer) {

        return Views.toMappedSet(getInEdgesOf(pointer), PointerFlowEdge::source);
    }

    @Override
    public Set<Pointer> getSuccsOf(Pointer pointer) {
        return Views.toMappedSet(getOutEdgesOf(pointer), PointerFlowEdge::target);
    }

    @Override
    public Set<PointerFlowEdge> getInEdgesOf(Pointer pointer) {
        return inEdges.get(pointer);
    }

    @Override
    public Set<PointerFlowEdge> getOutEdgesOf(Pointer pointer) {
        return outEdges.get(pointer);
    }

    @Override
    public Set<Pointer> getNodes() {
        return taintedPointers.stream().collect(Collectors.toUnmodifiableSet());
    }
}
