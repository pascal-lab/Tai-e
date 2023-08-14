package pascal.taie.analysis.pta.plugin.taint;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.analysis.graph.callgraph.CallGraph;
import pascal.taie.analysis.graph.flowgraph.FlowKind;
import pascal.taie.analysis.pta.core.cs.element.*;
import pascal.taie.analysis.pta.core.solver.PointerFlowEdge;
import pascal.taie.analysis.pta.core.solver.PointerFlowGraph;
import pascal.taie.analysis.pta.plugin.util.InvokeUtils;
import pascal.taie.analysis.pta.plugin.util.StrategyUtils;
import pascal.taie.ir.exp.Var;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.Sets;
import pascal.taie.util.graph.Reachability;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;


public class TPFGBuilder {
    private static final Logger logger = LogManager.getLogger(TPFGBuilder.class);

    private final PointerFlowGraph pfg;

    private final CallGraph<CSCallSite, CSMethod> callGraph;

    private final ClassHierarchy classHierarchy;

    private final CSManager csManager;

    private final TaintManager taintManager;

    private final Set<TaintFlow> taintFlows;

    private final boolean onlyApp;

    private final boolean onlyReachSink;

    public TPFGBuilder(PointerFlowGraph pfg,
                       CallGraph<CSCallSite, CSMethod> callGraph,
                       ClassHierarchy classHierarchy,
                       CSManager csManager, TaintManager taintManager,
                       Set<TaintFlow> taintFlows) {
        this.pfg = pfg;
        this.callGraph = callGraph;
        this.classHierarchy = classHierarchy;
        this.csManager = csManager;
        this.taintManager = taintManager;
        this.taintFlows = taintFlows;
        this.onlyApp = true;
        this.onlyReachSink = true;
    }

    public TPFGBuilder(PointerFlowGraph pfg,
                       CallGraph<CSCallSite, CSMethod> callGraph,
                       ClassHierarchy classHierarchy,
                       CSManager csManager, TaintManager taintManager,
                       Set<TaintFlow> taintFlows,
                       boolean onlyApp,
                       boolean onlyReachSink) {
        this.pfg = pfg;
        this.callGraph = callGraph;
        this.classHierarchy = classHierarchy;
        this.csManager = csManager;
        this.taintManager = taintManager;
        this.taintFlows = taintFlows;
        this.onlyApp = onlyApp;
        this.onlyReachSink = onlyReachSink;
    }

    private TaintPointerFlowGraph buildComplete() {
        Set<Pointer> taintedPointerSet = pfg.pointers()
                .filter(pointer -> (!onlyApp || isApp(pointer)) && hasTaint(pointer))
                .collect(Collectors.toSet());

        TaintPointerFlowGraph tpfg = new TaintPointerFlowGraph();
        taintedPointerSet.forEach(pointer -> pointer.getOutEdges().forEach(tpfg::addEdge));

        // connect base to this
        StrategyUtils.getMethod2CSCallSites(callGraph).values().stream()
                .filter(csCallSite -> !csCallSite.getCallSite().isStatic())
                .forEach(csCallSite -> addBase2ThisEdge(tpfg, csCallSite));

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

    private Set<CSObj> getTaintSet(Pointer pointer) {
        return pointer.objects()
                .filter(csObj -> taintManager.isTaint(csObj.getObject()))
                .collect(Collectors.toCollection(Sets::newHybridSet));
    }

    private boolean hasTaint(Pointer pointer) {
        return !getTaintSet(pointer).isEmpty();
    }

    private TaintPointerFlowGraph build() {
        TaintPointerFlowGraph complete = buildComplete();
        Set<Pointer> sourcePointers = findSourcePointers();
        Set<Pointer> sinkPointers = findSinkPointers();
        TaintPointerFlowGraph tpfg = new TaintPointerFlowGraph();
        Set<Pointer> PointersReachSink = null;
        if (onlyReachSink) {
            PointersReachSink = Sets.newHybridSet();
            Reachability<Pointer> reachability = new Reachability<>(complete);
            for (Pointer sinkPointer : sinkPointers) {
                PointersReachSink.addAll(reachability.nodesCanReach(sinkPointer));
            }
        }
        Set<Pointer> visitedPointers = Sets.newSet();
        Deque<Pointer> workList = new ArrayDeque<>(sourcePointers);
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
        Set<CSObj> baseTaintSet = getTaintSet(base);
        if (!baseTaintSet.isEmpty()) {
            callGraph.getCalleesOf(csCallSite).forEach(callee -> baseTaintSet.stream().map(csObj -> csObj.getObject().getType())
                    .map(taintedType -> classHierarchy.dispatch(taintedType, callee.getMethod().getRef()))
                    .filter(Objects::nonNull)
                    .forEach(taintCallee -> {
                        CSVar thisVar = csManager.getCSVar(callee.getContext(), callee.getMethod().getIR().getThis());
                        Set<CSObj> thisTaintSet = getTaintSet(thisVar);

                        if (Sets.haveOverlap(baseTaintSet, thisTaintSet)) {
                            tpfg.addEdge(FlowKind.THIS_PASSING, base, thisVar);
                        }
                    }));
        }
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
