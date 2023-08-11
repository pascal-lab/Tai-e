package pascal.taie.analysis.pta.plugin.taint;

import pascal.taie.analysis.pta.core.cs.element.Pointer;
import pascal.taie.analysis.pta.core.solver.PointerFlowEdge;
import pascal.taie.util.graph.Edge;
import pascal.taie.util.graph.Graph;

import java.util.Set;

public class TaintPointerFlowGraph implements Graph<Pointer> {
    @Override
    public Set<Pointer> getPredsOf(Pointer node) {
        return null;
    }

    @Override
    public Set<Pointer> getSuccsOf(Pointer node) {
        return null;
    }

    @Override
    public Set<PointerFlowEdge> getInEdgesOf(Pointer node) {
        return null;
    }

    @Override
    public Set<PointerFlowEdge> getOutEdgesOf(Pointer node) {
        return null;
    }

    @Override
    public Set<Pointer> getNodes() {
        return null;
    }
}
