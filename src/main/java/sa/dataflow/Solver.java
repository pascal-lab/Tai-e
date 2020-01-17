package sa.dataflow;

import sa.util.ReversedDirectedGraph;
import soot.toolkits.graph.DirectedGraph;

import java.util.HashMap;
import java.util.Map;

abstract class Solver<Domain, Result, Node> {

    protected DataFlowAnalysis<Domain, Result, Node> problem;

    /**
     * Out-flow value of each node.
     */
    protected Map<Node, Domain> outFlow;

    Result solve(DirectedGraph<Node> cfg) {
        if (!problem.isForward()) {
            // reverse control-flow graph for backward analysis
            cfg = new ReversedDirectedGraph<>(cfg);
        }
        initialize(cfg);
        solveFixedPoint(cfg);
        return problem.getAnalysisResult(this, cfg);
    }

    protected void initialize(DirectedGraph<Node> cfg) {
        outFlow = new HashMap<>();
        for (Node node : cfg) {
            outFlow.put(node, problem.newInitialValue());
        }
    }

    protected abstract void solveFixedPoint(DirectedGraph<Node> cfg);
}
