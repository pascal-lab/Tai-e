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

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class TaintObjectFlowGraph implements Graph<Pointer> {
    private final MultiMap<Pointer, PointerFlowEdge> inEdges = Maps.newMultiMap(4096);

    private final MultiMap<Pointer, PointerFlowEdge> outEdges = Maps.newMultiMap(4096);

    private final Set<Pointer> nodes = Sets.newSet();

    private final TaintPointerFlowGraph tpfg;

    private final Pointer sink;

    private final Solver solver;

    private Set<Entry> visited = Sets.newSet();

    private Map<Entry, Boolean> dfsCache = Maps.newMap();

    public TaintObjectFlowGraph(TaintPointerFlowGraph tpfg,
                                Pointer source,
                                CSObj concernedObj,
                                Pointer sink,
                                Solver solver) {
        assert source.getObjects().contains(concernedObj);
        this.tpfg = tpfg;
        this.sink = sink;
        this.solver = solver;
        init(source, concernedObj);
    }

    private void init(Pointer source, CSObj concernedObj) {
        dfs(new Entry(source, concernedObj));
        visited = null;
        dfsCache = null;
    }

    private boolean dfs(Entry entry) {
        if(!visited.add(entry)) {
            return false;
        }
        if(dfsCache.containsKey(entry)) {
            return dfsCache.get(entry);
        }
        Pointer pointer = entry.pointer;
        CSObj concernedObj = entry.concernedObj;
        assert pointer.getObjects().contains(concernedObj);
        PointsToSet pts = solver.makePointsToSet();
        pts.addObject(concernedObj);
        boolean canReachSink = pointer.equals(sink);
        for(PointerFlowEdge edge : tpfg.getOutEdgesOf(pointer)) {
            Set<CSObj> outObjs = applyTransfer(edge, concernedObj).getObjects();
            if(outObjs.stream().anyMatch(csObj -> dfs(new Entry(edge.target(), csObj)))) {
                addEdge(edge);
                canReachSink = true;
            }
        }
        visited.remove(entry);
        dfsCache.put(entry, canReachSink);
        return canReachSink;
    }

    private PointsToSet applyTransfer(PointerFlowEdge edge, CSObj csObj) {
        PointsToSet pts = solver.makePointsToSet();
        pts.addObject(csObj);
        PointsToSet result = solver.makePointsToSet();
        for(Transfer transfer : edge.getTransfers()) {
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
