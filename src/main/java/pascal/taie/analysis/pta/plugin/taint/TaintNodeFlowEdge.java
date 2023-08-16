package pascal.taie.analysis.pta.plugin.taint;


import pascal.taie.analysis.pta.core.solver.PointerFlowEdge;
import pascal.taie.util.graph.Edge;

public record TaintNodeFlowEdge(TaintNode source, TaintNode target, PointerFlowEdge pointerFlowEdge) implements Edge<TaintNode> {
}
