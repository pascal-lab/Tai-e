package pascal.taie.analysis.pta.plugin.taint;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.analysis.graph.flowgraph.FlowKind;
import pascal.taie.analysis.pta.core.cs.element.Pointer;
import pascal.taie.analysis.pta.core.solver.PointerFlowEdge;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Sets;
import pascal.taie.util.collection.Views;
import pascal.taie.util.graph.Graph;

import java.util.Set;
import java.util.stream.Collectors;

public class TaintPointerFlowGraph implements Graph<Pointer> {

    private static final Logger logger = LogManager.getLogger(TaintPointerFlowGraph.class);

    private final MultiMap<Pointer, PointerFlowEdge> inEdges = Maps.newMultiMap(4096);

    private final MultiMap<Pointer, PointerFlowEdge> outEdges = Maps.newMultiMap(4096);

    private final Set<Pointer> taintedPointers = Sets.newHybridSet();

    TaintPointerFlowGraph() {}

    public void addEdge(FlowKind kind, Pointer source, Pointer target) {
        PointerFlowEdge edge = new PointerFlowEdge(kind, source, target);
        outEdges.put(source, edge);
        inEdges.put(target, edge);
        taintedPointers.add(source);
        taintedPointers.add(target);
    }

    public void addEdge(PointerFlowEdge edge) {
        outEdges.put(edge.source(), edge);
        inEdges.put(edge.target(), edge);
        taintedPointers.add(edge.source());
        taintedPointers.add(edge.target());
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
