package sa.dataflow.solver;

import sa.dataflow.analysis.DataFlowAnalysis;
import sa.util.ReversedDirectedGraph;
import soot.toolkits.graph.DirectedGraph;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @param <Domain> Type for lattice values
 * @param <Node> Type for nodes of control-flow graph
 */
public abstract class Solver<Domain, Node> {

    protected DataFlowAnalysis<Domain, Node> problem;

    protected DirectedGraph<Node> cfg;

    /**
     * In-flow value of each node.
     */
    protected Map<Node, Domain> inFlow;

    /**
     * Out-flow value of each node.
     */
    protected Map<Node, Domain> outFlow;

    protected Solver(DataFlowAnalysis<Domain, Node> problem,
                     DirectedGraph<Node> cfg) {
        this.problem = problem;
        this.cfg = problem.isForward() ? cfg : new ReversedDirectedGraph<>(cfg);
    }

    public void solve() {
        initialize(cfg);
        solveFixedPoint(cfg);
    }

    /**
     * Returns the data-flow value before each node.
     */
    public Map<Node, Domain> getBeforeFlow() {
        return problem.isForward() ? inFlow : outFlow;
    }

    /**
     * Returns the data-flow value after each node.
     */
    public Map<Node, Domain> getAfterFlow() {
        return problem.isForward() ? outFlow : inFlow;
    }

    protected void initialize(DirectedGraph<Node> cfg) {
        for (Node node : cfg) {
            if (cfg.getHeads().contains(node)) {
                inFlow.put(node, problem.getEntryInitialValue());
            }
            outFlow.put(node, problem.newInitialValue());
        }
    }

    protected abstract void solveFixedPoint(DirectedGraph<Node> cfg);
}
