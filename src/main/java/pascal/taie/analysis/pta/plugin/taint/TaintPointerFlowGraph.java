package pascal.taie.analysis.pta.plugin.taint;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.analysis.graph.flowgraph.*;
import pascal.taie.analysis.pta.core.cs.element.*;
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

    private final Set<Pointer> sourcePointers;

    private final Set<Pointer> sinkPointers;

    public TaintPointerFlowGraph(Set<Pointer> sourcePointers, Set<Pointer> sinkPointers)
    {
        this.sourcePointers = Set.copyOf(sourcePointers);
        taintedPointers.addAll(sourcePointers);
        this.sinkPointers = Set.copyOf(sinkPointers);
        taintedPointers.addAll(sinkPointers);
    }

    private void addEdge(FlowKind kind, Pointer source, Pointer target) {
        PointerFlowEdge edge = new PointerFlowEdge(kind, source, target);
        outEdges.put(source, edge);
        inEdges.put(target, edge);
        taintedPointers.add(source);
        taintedPointers.add(target);
    }

    public Set<Pointer> getSourcePointers()
    {
        return sourcePointers;
    }

    public Set<Pointer> getSinkPointers()
    {
        return sinkPointers;
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
