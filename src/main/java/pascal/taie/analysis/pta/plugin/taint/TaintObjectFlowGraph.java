package pascal.taie.analysis.pta.plugin.taint;

import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.cs.element.Pointer;
import pascal.taie.analysis.pta.core.solver.PointerFlowEdge;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.core.solver.Transfer;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Sets;
import pascal.taie.util.collection.TwoKeyMap;
import pascal.taie.util.collection.Views;
import pascal.taie.util.graph.Graph;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Map;
import java.util.Set;

public class TaintObjectFlowGraph implements Graph<TaintNode> {

    private final MultiMap<TaintNode, TaintNodeFlowEdge> inEdges = Maps.newMultiMap(4096);

    private final MultiMap<TaintNode, TaintNodeFlowEdge> outEdges = Maps.newMultiMap(4096);

    private final Set<TaintNode> nodes = Sets.newSet(4096);

    private final TaintPointerFlowGraph tpfg;

    private final Solver solver;

    private final TaintNode sourceNode;

    private final TwoKeyMap<Pointer, CSObj, TaintNode> taintNodeMap = Maps.newTwoKeyMap();

    public TaintObjectFlowGraph(TaintPointerFlowGraph tpfg,
                                Pointer source,
                                CSObj concernedObj,
                                Solver solver) {
        assert source.getObjects().contains(concernedObj);
        this.tpfg = tpfg;
        this.solver = solver;
        this.sourceNode = new TaintNode(source, concernedObj);
        build(sourceNode);
    }

    private void build(TaintNode node) {
        Set<TaintNode> visited = Sets.newSet();
        Deque<TaintNode> workList = new ArrayDeque<>();
        workList.add(node);

        while (!workList.isEmpty()) {
            TaintNode curr = workList.poll();
            if (visited.add(curr)) {
                Pointer pointer = curr.pointer();
                CSObj taintObj = curr.taintObj();
                assert pointer.getObjects().contains(taintObj);
                for (PointerFlowEdge pointerFlowEdge : tpfg.getOutEdgesOf(pointer)) {
                    Pointer target = pointerFlowEdge.target();
                    for(CSObj outObj : applyTransfer(pointerFlowEdge, taintObj)) {
                        if(target.getObjects().contains(outObj)) {
                            TaintNode newNode = taintNodeMap.computeIfAbsent(target, outObj, TaintNode::new);
                            addEdge(new TaintNodeFlowEdge(curr, newNode, pointerFlowEdge));
                            workList.add(newNode);
                        }
                    }
                }
            }
        }
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

    public void addEdge(TaintNodeFlowEdge edge) {
        outEdges.put(edge.source(), edge);
        inEdges.put(edge.target(), edge);
        nodes.add(edge.source());
        nodes.add(edge.target());
    }

    public TaintNode getSourceNode() {
        return sourceNode;
    }

    public Collection<TaintNode> getTaintNode(Pointer pointer) {
        return taintNodeMap.getOrDefault(pointer, Map.of()).values();
    }

    @Override
    public Set<TaintNode> getPredsOf(TaintNode node) {
        return Views.toMappedSet(getInEdgesOf(node), TaintNodeFlowEdge::source);
    }

    @Override
    public Set<TaintNode> getSuccsOf(TaintNode node) {
        return Views.toMappedSet(getOutEdgesOf(node), TaintNodeFlowEdge::target);
    }

    @Override
    public Set<TaintNodeFlowEdge> getInEdgesOf(TaintNode node) {
        return inEdges.get(node);
    }

    @Override
    public Set<TaintNodeFlowEdge> getOutEdgesOf(TaintNode node) {
        return outEdges.get(node);
    }

    @Override
    public Set<TaintNode> getNodes() {
        return Collections.unmodifiableSet(nodes);
    }
}
