package sa.dataflow.solver;

import sa.dataflow.analysis.IPDataFlowAnalysis;
import sa.icfg.Edge;
import sa.icfg.ICFG;

import java.util.Map;

public abstract class IPSolver<Domain, Method, Node> {

    protected IPDataFlowAnalysis<Domain, Method, Node> problem;

    protected ICFG<Method, Node> icfg;

    /**
     * In-flow value of each node.
     */
    protected Map<Node, Domain> inFlow;

    /**
     * Out-flow value of each node.
     */
    protected Map<Node, Domain> outFlow;

    /**
     * Flow value for ICFG edges.
     */
    protected Map<Edge<Node>, Domain> edgeFlow;

    protected IPSolver(IPDataFlowAnalysis<Domain, Method, Node> problem,
                     ICFG<Method, Node> icfg) {
        this.problem = problem;
        this.icfg = icfg;
    }

    public void solve() {
        initialize(icfg);
        solveFixedPoint(icfg);
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

    protected void initialize(ICFG<Method, Node> icfg) {
        for (Node node : icfg) {
            if (icfg.getHeads().contains(node)) {
                inFlow.put(node, problem.getEntryInitialValue());
            }
            outFlow.put(node, problem.newInitialValue());
            icfg.getInEdgesOf(node)
                    .forEach(edge ->
                            edgeFlow.put(edge, problem.newInitialValue()));
        }
    }

    protected abstract void solveFixedPoint(ICFG<Method, Node> icfg);
}
