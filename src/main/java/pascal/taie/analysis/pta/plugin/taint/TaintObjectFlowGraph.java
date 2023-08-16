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
import pascal.taie.util.collection.Views;
import pascal.taie.util.graph.Graph;

import java.util.*;

public class TaintObjectFlowGraph implements Graph<Pointer> {
    private final MultiMap<Pointer, PointerFlowEdge> inEdges = Maps.newMultiMap(4096);

    private final MultiMap<Pointer, PointerFlowEdge> outEdges = Maps.newMultiMap(4096);

    private final Set<Pointer> nodes = Sets.newSet();

    private final TaintPointerFlowGraph tpfg;

    private final Pointer sink;

    private final Solver solver;


    public TaintObjectFlowGraph(TaintPointerFlowGraph tpfg,
                                Pointer source,
                                CSObj concernedObj,
                                Pointer sink,
                                Solver solver) {
        assert source.getObjects().contains(concernedObj);
        this.tpfg = tpfg;
        this.sink = sink;
        this.solver = solver;
        build(new Entry(source, concernedObj));
    }

    private void build(Entry entry) {
        Set<Entry> visited = Sets.newSet();
        Map<Entry, Boolean> dfsCache = Maps.newMap();
        Deque<Entry> stack = new ArrayDeque<>();
        stack.push(entry);

        while (!stack.isEmpty()) {
            Entry curr = stack.pop();
            Pointer pointer = curr.pointer;
            CSObj concernedObj = curr.concernedObj;
            assert pointer.getObjects().contains(concernedObj);
            if (visited.add(curr)) {
                stack.push(curr);
                for (PointerFlowEdge edge : tpfg.getOutEdgesOf(pointer)) {
                    Set<CSObj> outObjs = applyTransfer(edge, concernedObj).getObjects();
                    for (CSObj csObj : outObjs) {
                        Entry newEntry = new Entry(edge.target(), csObj);
                        if (!dfsCache.containsKey(newEntry)) {
                            stack.push(newEntry);
                        }
                    }
                }
            } else {
                boolean canReachSink = pointer.equals(sink);
                for (PointerFlowEdge edge : tpfg.getOutEdgesOf(pointer)) {
                    Set<CSObj> outObjs = applyTransfer(edge, concernedObj).getObjects();
                    if (outObjs.stream().anyMatch(csObj ->
                            dfsCache.getOrDefault(new Entry(edge.target(), csObj), false))) {
                        addEdge(edge);
                        canReachSink = true;
                    }
                }
                dfsCache.put(curr, canReachSink);
                visited.remove(curr);
            }
        }
    }

    private PointsToSet applyTransfer(PointerFlowEdge edge, CSObj csObj) {
        PointsToSet pts = solver.makePointsToSet();
        pts.addObject(csObj);
        PointsToSet result = solver.makePointsToSet();
        for (Transfer transfer : edge.getTransfers()) {
            result.addAll(transfer.apply(edge, pts));
        }
        return result;
    }

    record Entry(Pointer pointer, CSObj concernedObj) {

    }

    public void addEdge(PointerFlowEdge edge) {
        outEdges.put(edge.source(), edge);
        inEdges.put(edge.target(), edge);
        nodes.add(edge.source());
        nodes.add(edge.target());
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
        return Collections.unmodifiableSet(nodes);
    }
}
