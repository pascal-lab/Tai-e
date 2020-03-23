package pascal.dataflow.solver;

import pascal.dataflow.analysis.DataFlowAnalysis;
import soot.toolkits.graph.DirectedGraph;

public enum SolverFactory {

    INSTANCE;

    public static SolverFactory v() {
        return INSTANCE;
    }

    public <Domain, Node>
    Solver<Domain, Node> newSolver(DataFlowAnalysis<Domain, Node> problem,
                                   DirectedGraph<Node> cfg) {
        return new IterativeSolver<>(problem, cfg);
    }
}
