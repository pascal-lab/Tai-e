package pascal.taie.analysis.pta.plugin.taint;

import pascal.taie.analysis.pta.core.cs.element.Pointer;
import pascal.taie.analysis.pta.core.solver.PointerFlowEdge;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Sets;
import pascal.taie.util.collection.Views;
import pascal.taie.util.graph.Graph;

import java.util.Collections;
import java.util.Set;

public class TaintPointerFlowGraph implements Graph<Pointer> {

    private final MultiMap<Pointer, PointerFlowEdge> inEdges = Maps.newMultiMap(4096);

    private final MultiMap<Pointer, PointerFlowEdge> outEdges = Maps.newMultiMap(4096);

    private final Set<Pointer> taintedPointers = Sets.newHybridSet();

    private final Set<Pointer> sourcePointers;

    private final Set<Pointer> sinkPointers;

    public TaintPointerFlowGraph(Set<Pointer> sourcePointers, Set<Pointer> sinkPointers) {
        this.sourcePointers = Set.copyOf(sourcePointers);
        taintedPointers.addAll(sourcePointers);
        this.sinkPointers = Set.copyOf(sinkPointers);
        taintedPointers.addAll(sinkPointers);
    }

    public void addEdge(PointerFlowEdge edge) {
        outEdges.put(edge.source(), edge);
        inEdges.put(edge.target(), edge);
        taintedPointers.add(edge.source());
        taintedPointers.add(edge.target());
    }

    public Set<Pointer> getSourcePointers() {
        return sourcePointers;
    }

    public Set<Pointer> getSinkPointers() {
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
        return Collections.unmodifiableSet(taintedPointers);
    }
}
