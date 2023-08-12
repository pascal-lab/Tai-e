package pascal.taie.analysis.pta.plugin.taint;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.analysis.graph.callgraph.CallGraph;
import pascal.taie.analysis.graph.flowgraph.FlowKind;
import pascal.taie.analysis.pta.core.cs.element.*;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.core.solver.PointerFlowEdge;
import pascal.taie.analysis.pta.core.solver.PointerFlowGraph;
import pascal.taie.analysis.pta.plugin.util.InvokeUtils;
import pascal.taie.analysis.pta.plugin.util.StrategyUtils;
import pascal.taie.ir.exp.Var;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Sets;
import pascal.taie.util.graph.Reachability;


import java.util.*;
import java.util.stream.Collectors;


public class TPFGBuilder {
    private static final Logger logger = LogManager.getLogger(TPFGBuilder.class);

    private final PointerFlowGraph pfg;

    private final CallGraph<CSCallSite, CSMethod> callGraph;

    private final CSManager csManager;

    private final TaintManager taintManager;

    private final Set<TaintFlow> taintFlows;

    private final boolean onlyApp;

    private final boolean onlyReachSink;

    private Map<Pointer, Set<CSObj>> pointer2TaintSet;

    public TPFGBuilder(PointerFlowGraph pfg,
                       CallGraph<CSCallSite, CSMethod> callGraph,
                       CSManager csManager, TaintManager taintManager,
                       Set<TaintFlow> taintFlows) {
        this.pfg = pfg;
        this.callGraph = callGraph;
        this.csManager = csManager;
        this.taintManager = taintManager;
        this.taintFlows = taintFlows;
        this.onlyApp = true;
        this.onlyReachSink = true;
    }

    public TPFGBuilder(PointerFlowGraph pfg,
                       CallGraph<CSCallSite, CSMethod> callGraph,
                       CSManager csManager, TaintManager taintManager,
                       Set<TaintFlow> taintFlows,
                       boolean onlyApp,
                       boolean onlyReachSink) {
        this.pfg = pfg;
        this.callGraph = callGraph;
        this.csManager = csManager;
        this.taintManager = taintManager;
        this.taintFlows = taintFlows;
        this.onlyApp = onlyApp;
        this.onlyReachSink = onlyReachSink;
    }

    private TaintPointerFlowGraph buildComplete() {
        // collect source pointers
        Set<Pointer> sourcePointers = findSourcePointers();

        //collect sink Pointers
        Set<Pointer> sinkPointers = findSinkPointers();

        //build Taint Pointer Flow Graph
        pointer2TaintSet = Maps.newMap();
        TaintPointerFlowGraph tpfg = new TaintPointerFlowGraph(sourcePointers, sinkPointers);
        Set<Pointer> visitedPointers = Sets.newSet();
        Deque<Pointer> workList = new ArrayDeque<>(sourcePointers);
        while (!workList.isEmpty()) {
            Pointer pointer = workList.poll();
            if (visitedPointers.add(pointer)) {
                getOutEdges(pointer).forEach(edge ->
                {
                    Pointer target = edge.target();
                    if (!onlyApp || isApp(edge.target())) {
                        tpfg.addEdge(edge.kind(), pointer, target);
                        if (!visitedPointers.contains(target)) {
                            workList.add(target);
                        }
                    }

                });
            }
        }

        // connect base to this
        StrategyUtils.getMethod2CSCallSites(callGraph).values().stream()
                .filter(csCallSite -> !csCallSite.getCallSite().isStatic())
                .forEach(csCallSite -> addBase2ThisEdge(tpfg, csCallSite));

        pointer2TaintSet = null;

        return tpfg;
    }

    private Set<Pointer> findSourcePointers() {
        Set<Pointer> sourcePointers = Sets.newHybridSet();
        taintManager.getTaintObjs()
                .stream()
                .map(taintManager::getSourcePoint)
                .forEach(p -> {
                    Var sourceVar = null;
                    if (p instanceof CallSourcePoint csp) {
                        sourceVar = InvokeUtils.getVar(
                                csp.sourceCall(), csp.index());
                    } else if (p instanceof ParamSourcePoint psp) {
                        sourceVar = psp.sourceMethod().getIR()
                                .getParam(psp.index());
                    } else if (p instanceof FieldSourcePoint fsp) {
                        sourceVar = fsp.loadField().getLValue();
                    }
                    if (sourceVar != null) {
                        sourcePointers.addAll(csManager.getCSVarsOf(sourceVar));
                    }
                });
        return sourcePointers;
    }

    private Set<Pointer> findSinkPointers() {
        Set<Pointer> sinkPointers = Sets.newHybridSet();
        taintFlows.forEach(taintFlow -> {
            SinkPoint sinkPoint = taintFlow.sinkPoint();
            var sinkVar = InvokeUtils.getVar(sinkPoint.sinkCall(), sinkPoint.index());
            sinkPointers.addAll(csManager.getCSVarsOf(sinkVar));
        });
        return sinkPointers;
    }

    private List<PointerFlowEdge> getOutEdges(Pointer source) {
        Set<CSObj> sourceTaintSet = getTaintSet(source);
        List<PointerFlowEdge> edges = new ArrayList<>();

        // collect PFG edges
        for (PointerFlowEdge pointerFlowEdge : pfg.getOutEdgesOf(source)) {
            switch (pointerFlowEdge.kind()) {
                case LOCAL_ASSIGN, INSTANCE_STORE, ARRAY_STORE,
                        THIS_PASSING, PARAMETER_PASSING, OTHER -> {
                    edges.add(pointerFlowEdge);
                }
                case CAST, INSTANCE_LOAD, ARRAY_LOAD, RETURN -> {
                    Set<CSObj> targetTaintSet = getTaintSet(pointerFlowEdge.target());
                    if (Sets.haveOverlap(sourceTaintSet, targetTaintSet)) {
                        edges.add(pointerFlowEdge);
                    }
                }
            }
        }
        return edges;
    }

    private Set<CSObj> getTaintSet(Pointer pointer) {
        Set<CSObj> taintSet = pointer2TaintSet.get(pointer);
        if (taintSet == null) {
            taintSet = pointer.objects()
                    .filter(csObj -> taintManager.isTaint(csObj.getObject()))
                    .collect(Collectors.toCollection(Sets::newHybridSet));

            if (taintSet.isEmpty()) {
                taintSet = Set.of();
            }
            pointer2TaintSet.put(pointer, taintSet);
        }
        return taintSet;
    }

    public TaintPointerFlowGraph build() {
        TaintPointerFlowGraph complete = buildComplete();
        Set<Pointer> sourcePointers = complete.getSourcePointers();
        Set<Pointer> sinkPointers = complete.getSinkPointers();
        TaintPointerFlowGraph tpfg = new TaintPointerFlowGraph(sourcePointers, sinkPointers);
        Set<Pointer> PointersReachSink = null;
        if (onlyReachSink) {
            PointersReachSink = Sets.newHybridSet();
            Reachability<Pointer> reachability = new Reachability<>(complete);
            for (Pointer sinkPointer : sinkPointers) {
                PointersReachSink.addAll(reachability.nodesCanReach(sinkPointer));
            }
        }
        Set<Pointer> visitedPointers = Sets.newSet();
        Deque<Pointer> workList = new ArrayDeque<>(complete.getSourcePointers());
        while (!workList.isEmpty()) {
            Pointer pointer = workList.poll();
            if (visitedPointers.add(pointer)) {
                for (PointerFlowEdge edge : complete.getOutEdgesOf(pointer)) {
                    Pointer target = edge.target();
                    if (!onlyReachSink || PointersReachSink.contains(target)) {
                        tpfg.addEdge(edge);
                        if (!visitedPointers.contains(target)) {
                            workList.add(target);
                        }
                    }
                }
            }
        }
        return tpfg;
    }

    private void addBase2ThisEdge(TaintPointerFlowGraph tpfg, CSCallSite csCallSite) {
        CSVar base = StrategyUtils.getCSVar(csManager, csCallSite, InvokeUtils.BASE);
        callGraph.getCalleesOf(csCallSite).forEach(callee -> {
            CSVar thisVar = csManager.getCSVar(callee.getContext(), callee.getMethod().getIR().getThis());
            Set<Type> thisObjTypes = thisVar.getObjects().stream()
                    .map(CSObj::getObject)
                    .map(Obj::getType)
                    .collect(Collectors.toSet());

            if (Sets.haveOverlap(getTaintedTypes(base), thisObjTypes)) {
                tpfg.addEdge(FlowKind.THIS_PASSING, base, thisVar);
            }
        });
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

    private static boolean isApp(Pointer pointer) {
        if (pointer instanceof CSVar csVar) {
            return csVar.getVar().getMethod().isApplication();
        } else if (pointer instanceof InstanceField iField) {
            return iField.getField().isApplication();
        } else if (pointer instanceof ArrayIndex arrayIndex) {
            return arrayIndex.getArray().getObject().getContainerMethod().stream().anyMatch(JMethod::isApplication);
        } else if (pointer instanceof StaticField staticField) {
            // In the original TFGBuilder, this condition check was not present. I'm not sure if this check is necessary.
            return staticField.getField().isApplication();
        } else {
            return false;
        }
    }
}
