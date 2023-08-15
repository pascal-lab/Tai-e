package pascal.taie.analysis.pta.plugin.taint;

import pascal.taie.analysis.graph.callgraph.CallGraph;
import pascal.taie.analysis.graph.callgraph.CallGraphs;
import pascal.taie.analysis.graph.flowgraph.FlowKind;
import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.ArrayIndex;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSManager;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.cs.element.InstanceField;
import pascal.taie.analysis.pta.core.cs.element.Pointer;
import pascal.taie.analysis.pta.core.cs.element.StaticField;
import pascal.taie.analysis.pta.core.cs.selector.ContextSelector;
import pascal.taie.analysis.pta.core.solver.PointerFlowEdge;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.core.solver.Transfer;
import pascal.taie.analysis.pta.plugin.util.InvokeUtils;
import pascal.taie.analysis.pta.plugin.util.StrategyUtils;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.exp.Var;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Sets;
import pascal.taie.util.graph.Reachability;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


public class TPFGBuilder {

    private final Solver solver;

    private final TaintManager taintManager;

    private final CSManager csManager;

    private final Set<TaintFlow> taintFlows;

    private final boolean onlyApp;

    private final boolean onlyReachSink;

    private Map<Pointer, Set<CSObj>> pointer2TaintSet;

    public TPFGBuilder(Solver solver, TaintManager taintManager, Set<TaintFlow> taintFlows) {
        this(solver, taintManager, taintFlows, true, true);
    }

    public TPFGBuilder(Solver solver,
                       TaintManager taintManager,
                       Set<TaintFlow> taintFlows,
                       boolean onlyApp,
                       boolean onlyReachSink) {
        this.solver = solver;
        this.taintManager = taintManager;
        this.taintFlows = taintFlows;
        this.onlyApp = onlyApp;
        this.onlyReachSink = onlyReachSink;
        this.csManager = solver.getCSManager();
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

    private TaintPointerFlowGraph buildComplete() {
        this.pointer2TaintSet = Maps.newMap();
        Set<Pointer> sourcePointers = getSourcePointers();
        Set<Pointer> sinkPointers = getSinkPointers();
        TaintPointerFlowGraph tpfg = new TaintPointerFlowGraph(sourcePointers, sinkPointers);

        Set<Pointer> taintPointers = solver.getPointerFlowGraph().getNodes().stream()
                .filter(pointer -> hasTaint(pointer) && (!onlyApp || isApp(pointer)))
                .collect(Collectors.toSet());

        taintPointers.stream()
                .map(this::getOutEdges)
                .flatMap(Collection::stream)
                .forEach(tpfg::addEdge);

        ContextSelector contextSelector = solver.getContextSelector();
        CallGraph<CSCallSite, CSMethod> callGraph = solver.getCallGraph();
        // connect base to this
        for (Pointer pointer : taintPointers) {
            if (pointer instanceof CSVar csVar) {
                Context context = csVar.getContext();
                Var var = csVar.getVar();
                var.getInvokes().stream()
                        .map(invoke -> csManager.getCSCallSite(context, invoke))
                        .forEach(csCallSite -> {
                            CSVar csBase = StrategyUtils.getCSVar(csManager, csCallSite, InvokeUtils.BASE);
                            assert csBase != null;
                            getTaintSet(csBase).forEach(taintObj -> {
                                JMethod callee = CallGraphs.resolveCallee(
                                        taintObj.getObject().getType(), csCallSite.getCallSite());
                                if (callee != null) {
                                    assert !callee.isStatic();
                                    Context calleeContext = contextSelector.selectContext(
                                            csCallSite, taintObj, callee);
                                    CSMethod csCallee = csManager.getCSMethod(calleeContext, callee);
                                    assert callGraph.getCalleesOf(csCallSite).contains(csCallee);
                                    CSVar csThis = csManager.getCSVar(calleeContext, callee.getIR().getThis());
                                    PointerFlowEdge edge = new PointerFlowEdge(FlowKind.THIS_PASSING, csBase, csThis);
                                    edge.addTransfer(getTransfer(taintObj));
                                    tpfg.addEdge(edge);
                                }
                            });
                        });
            }
        }

        this.pointer2TaintSet = null;
        return tpfg;
    }

    public TaintPointerFlowGraph build() {
        TaintPointerFlowGraph complete = buildComplete();
        Set<Pointer> sourcePointers = complete.getSourcePointers();
        Set<Pointer> sinkPointers = complete.getSinkPointers();
        TaintPointerFlowGraph tpfg = new TaintPointerFlowGraph(sourcePointers, sinkPointers);
        Set<Pointer> nodesReachSink = null;
        if (onlyReachSink) {
            nodesReachSink = Sets.newHybridSet();
            Reachability<Pointer> reachability = new Reachability<>(complete);
            for (Pointer sink : sinkPointers) {
                nodesReachSink.addAll(reachability.nodesCanReach(sink));
            }
        }
        Set<Pointer> visitedNodes = Sets.newSet();
        Deque<Pointer> workList = new ArrayDeque<>(sourcePointers);
        while (!workList.isEmpty()) {
            Pointer pointer = workList.poll();
            if (visitedNodes.add(pointer)) {
                for (PointerFlowEdge edge : complete.getOutEdgesOf(pointer)) {
                    Pointer target = edge.target();
                    if (!onlyReachSink || nodesReachSink.contains(target)) {
                        tpfg.addEdge(edge);
                        if (!visitedNodes.contains(target)) {
                            workList.add(target);
                        }
                    }
                }
            }
        }
        return tpfg;
    }

    private Transfer getTransfer(CSObj base) {
        return ((edge, input) -> {
            PointsToSet pts = solver.makePointsToSet();
            if(input.contains(base)) {
                pts.addObject(base);
            }
            return pts;
        });
    }

    private Set<CSObj> getTaintSet(Pointer pointer) {
        Set<CSObj> taintSet = pointer2TaintSet.get(pointer);
        if (taintSet == null) {
            taintSet = pointer.objects()
                    .filter(csObj -> taintManager.isTaint(csObj.getObject()))
                    .collect(Sets::newHybridSet, Set::add, Set::addAll);
            if (taintSet.isEmpty()) {
                taintSet = Set.of();
            }
            pointer2TaintSet.put(pointer, taintSet);
        }
        return taintSet;
    }

    private boolean hasTaint(Pointer pointer) {
        return !getTaintSet(pointer).isEmpty();
    }

    private List<PointerFlowEdge> getOutEdges(Pointer pointer) {
        Set<CSObj> taintSet = getTaintSet(pointer);
        PointsToSet pts = solver.makePointsToSet();
        taintSet.forEach(pts::addObject);
        return pointer.getOutEdges().stream()
                .filter(edge -> edge.getTransfers().stream()
                        .anyMatch(tf -> !tf.apply(edge, pts).isEmpty()))
                .toList();
    }

    private Set<Pointer> getSourcePointers() {
        Set<Pointer> sourcePointers = Sets.newHybridSet();
        taintManager.getTaintObjs()
                .stream()
                .map(taintManager::getSourcePoint)
                .forEach(p -> {
                    Var sourceVar = null;
                    if (p instanceof CallSourcePoint csp) {
                        sourceVar = InvokeUtils.getVar(csp.sourceCall(), csp.index());
                    } else if (p instanceof ParamSourcePoint psp) {
                        sourceVar = psp.sourceMethod().getIR().getParam(psp.index());
                    } else if (p instanceof FieldSourcePoint fsp) {
                        sourceVar = fsp.loadField().getLValue();
                    }
                    if (sourceVar != null) {
                        csManager.getCSVarsOf(sourceVar).stream()
                                .filter(this::hasTaint)
                                .forEach(sourcePointers::add);
                    }
                });
        return sourcePointers;
    }

    private Set<Pointer> getSinkPointers() {
        Set<Pointer> sinkPointers = Sets.newHybridSet();
        taintFlows.forEach(taintFlow -> {
            SinkPoint sinkPoint = taintFlow.sinkPoint();
            Var sinkVar = InvokeUtils.getVar(sinkPoint.sinkCall(), sinkPoint.index());
            csManager.getCSVarsOf(sinkVar).stream()
                    .filter(this::hasTaint)
                    .forEach(sinkPointers::add);
        });
        return sinkPointers;
    }
}
