package bamboo.dataflow.solver;

import bamboo.dataflow.analysis.DataFlowAnalysis;
import bamboo.util.ReversedDirectedGraph;
import soot.toolkits.graph.DirectedGraph;

import java.util.Map;

/**
 *
 * @param <Domain> Type for lattice values
 * @param <Node> Type for nodes of control-flow graph
 */
public abstract class Solver<Domain, Node> {

    protected DataFlowAnalysis<Domain, Node> analysis;

    protected DirectedGraph<Node> cfg;

    /**
     * In-flow value of each node.
     */
    protected Map<Node, Domain> inFlow;

    /**
     * Out-flow value of each node.
     */
    protected Map<Node, Domain> outFlow;

    protected Solver(DataFlowAnalysis<Domain, Node> analysis,
                     DirectedGraph<Node> cfg) {
        this.analysis = analysis;
        this.cfg = analysis.isForward() ? cfg : new ReversedDirectedGraph<>(cfg);
    }

    public void solve() {
        initialize(cfg);
        solveFixedPoint(cfg);
    }

    /**
     * Returns the data-flow value before each node.
     */
    public Map<Node, Domain> getBeforeFlow() {
        return analysis.isForward() ? inFlow : outFlow;
    }

    /**
     * Returns the data-flow value after each node.
     */
    public Map<Node, Domain> getAfterFlow() {
        return analysis.isForward() ? outFlow : inFlow;
    }

    protected void initialize(DirectedGraph<Node> cfg) {
        for (Node node : cfg) {
            if (cfg.getHeads().contains(node)) {
                inFlow.put(node, analysis.getEntryInitialFlow(node));
            }
            outFlow.put(node, analysis.newInitialFlow());
        }
    }

    protected abstract void solveFixedPoint(DirectedGraph<Node> cfg);
}
