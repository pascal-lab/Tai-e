package sa.dataflow.solver;

import sa.dataflow.analysis.DataFlowAnalysis;
import sa.util.ReversedDirectedGraph;
import soot.toolkits.graph.DirectedGraph;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @param <Domain> Type for lattice values
 * @param <Result> Type for analysis results
 * @param <Node> Type for nodes of control-flow graph
 */
public abstract class Solver<Domain, Result, Node> {

    protected DataFlowAnalysis<Domain, Result, Node> problem;

    /**
     * Out-flow value of each node.
     */
    protected Map<Node, Domain> outFlow;

    protected Solver(DataFlowAnalysis<Domain, Result, Node> problem) {
        this.problem = problem;
    }

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
            if (cfg.getHeads().contains(node)) {
                outFlow.put(node, problem.getEntryInitialValue(node));
            } else {
                outFlow.put(node, problem.newInitialValue(cfg));
            }
        }
    }

    protected abstract void solveFixedPoint(DirectedGraph<Node> cfg);
}
