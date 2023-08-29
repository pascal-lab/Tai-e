package pascal.taie.analysis.pta.plugin.taint;

import pascal.taie.analysis.graph.callgraph.CallGraphs;
import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.cs.element.Pointer;
import pascal.taie.analysis.pta.core.solver.PointerFlowEdge;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.core.solver.Transfer;
import pascal.taie.analysis.pta.plugin.util.InvokeUtils;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Sets;
import pascal.taie.util.collection.TwoKeyMap;
import pascal.taie.util.graph.Reachability;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;
import java.util.stream.Collectors;

public class TOFGBuilder {

    private final TaintPointerFlowGraph tpfg;

    private final Solver solver;

    private final Set<TaintNode> completeSinkNodes;

    private final Set<JMethod> sinkMethods;

    private final MultiMap<Var, SinkPoint> sinkVar2SinkPoint;

    public TOFGBuilder(TaintPointerFlowGraph tpfg,
                       Solver solver,
                       TaintManager taintManager,
                       TaintConfig taintConfig,
                       Set<TaintFlow> taintFlows) {
        this.tpfg = tpfg;
        this.solver = solver;

        this.sinkMethods = taintConfig.sinks().stream()
                .map(Sink::method)
                .collect(Collectors.toUnmodifiableSet());
        this.sinkVar2SinkPoint = Maps.newMultiMap();
        for (TaintFlow taintFlow : taintFlows) {
            SinkPoint sinkPoint = taintFlow.sinkPoint();
            Invoke sinkCall = sinkPoint.sinkCall();
            Var sinkVar = InvokeUtils.getVar(sinkCall, sinkPoint.index());
            sinkVar2SinkPoint.put(sinkVar, sinkPoint);
        }

        this.completeSinkNodes = tpfg.getSinkPointers().stream()
                .flatMap(sinkPointer -> sinkPointer.objects()
                        .filter(csObj -> taintManager.isTaint(csObj.getObject()))
                        .map(csObj -> new TaintNode(sinkPointer, csObj)))
                .collect(Collectors.toUnmodifiableSet());
    }

    private TaintObjectFlowGraph buildComplete(Pointer source, CSObj concernedObj) {
        TaintNode sourceNode = new TaintNode(source, concernedObj);
        TaintObjectFlowGraph complete = new TaintObjectFlowGraph(sourceNode, completeSinkNodes);

        TwoKeyMap<Pointer, CSObj, TaintNode> taintNodeMap = Maps.newTwoKeyMap();
        Set<TaintNode> visited = Sets.newSet();
        Deque<TaintNode> workList = new ArrayDeque<>();
        workList.add(sourceNode);

        while (!workList.isEmpty()) {
            TaintNode curr = workList.poll();
            if (visited.add(curr)) {
                Pointer pointer = curr.pointer();
                CSObj taintObj = curr.taintObj();
                assert pointer.getObjects().contains(taintObj);
                for (PointerFlowEdge pointerFlowEdge : tpfg.getOutEdgesOf(pointer)) {
                    Pointer target = pointerFlowEdge.target();
                    for (CSObj outObj : applyTransfer(pointerFlowEdge, taintObj)) {
                        if (target.getObjects().contains(outObj)) {
                            TaintNode newNode = taintNodeMap.computeIfAbsent(target, outObj, TaintNode::new);
                            complete.addEdge(new TaintObjectFlowEdge(curr, newNode, pointerFlowEdge));
                            workList.add(newNode);
                        }
                    }
                }
            }
        }

        return complete;
    }

    public TaintObjectFlowGraph build(Pointer source, CSObj concernedObj) {
        assert source.getObjects().contains(concernedObj);
        TaintObjectFlowGraph complete = buildComplete(source, concernedObj);
        // Collect reachable and real sink nodes
        TaintNode sourceNode = complete.getSourceNode();
        Set<TaintNode> sinkNodes = complete.getSinkNodes().stream()
                .filter(node -> complete.getInDegreeOf(node) > 0 && isRealSinkNode(node))
                .collect(Collectors.toUnmodifiableSet());
        Set<TaintNode> nodesReachSink = new Reachability<>(complete).nodesCanReach(sinkNodes);
        TaintObjectFlowGraph tofg = new TaintObjectFlowGraph(complete.getSourceNode(), sinkNodes);

        Set<TaintNode> visitedNodes = Sets.newSet();
        Deque<TaintNode> workList = new ArrayDeque<>();
        workList.add(sourceNode);
        while (!workList.isEmpty()) {
            TaintNode curr = workList.poll();
            if (visitedNodes.add(curr)) {
                for (TaintObjectFlowEdge edge : complete.getOutEdgesOf(curr)) {
                    TaintNode target = edge.target();
                    if (nodesReachSink.contains(target)) {
                        tofg.addEdge(edge);
                        if (!visitedNodes.contains(target)) {
                            workList.add(target);
                        }
                    }
                }
            }
        }
        return tofg;
    }

    private PointsToSet applyTransfer(PointerFlowEdge edge, CSObj taintObj) {
        PointsToSet pts = solver.makePointsToSet();
        pts.addObject(taintObj);
        PointsToSet result = solver.makePointsToSet();
        for (Transfer transfer : edge.getTransfers()) {
            result.addAll(transfer.apply(edge, pts));
        }
        return result;
    }

    private boolean isRealSinkNode(TaintNode node) {
        if (node.pointer() instanceof CSVar csVar) {
            for (SinkPoint sinkPoint : sinkVar2SinkPoint.get(csVar.getVar())) {
                if (sinkPoint.index() != InvokeUtils.BASE) {
                    return true;
                }
                JMethod callee = CallGraphs.resolveCallee(
                        node.taintObj().getObject().getType(),
                        sinkPoint.sinkCall());
                if (callee != null) {
                    return sinkMethods.contains(callee);
                }
            }
        }
        return false;
    }
}
