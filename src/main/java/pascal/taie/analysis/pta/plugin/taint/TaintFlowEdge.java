package pascal.taie.analysis.pta.plugin.taint;

import pascal.taie.analysis.graph.flowgraph.FlowKind;
import pascal.taie.analysis.graph.flowgraph.Node;
import pascal.taie.util.graph.Edge;

public record TaintFlowEdge(FlowKind kind, Node source, Node target,
                            boolean isInferred) implements Edge<Node> {

}
