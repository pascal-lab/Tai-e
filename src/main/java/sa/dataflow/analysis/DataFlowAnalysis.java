package sa.dataflow.analysis;

import sa.dataflow.solver.Solver;
import soot.toolkits.graph.DirectedGraph;

/**
 *
 * @param <Domain> Type for lattice values
 * @param <Result> Type for analysis results
 * @param <Node> Type for nodes of control-flow graph
 */
public interface DataFlowAnalysis<Domain, Result, Node> {

    /**
     * Returns whether the analysis is forward.
     */
    boolean isForward();

    /**
     * Returns initial value for entry node.
     */
    Domain getEntryInitialValue(Node entry);

    /**
     * Returns initial value for other nodes.
     */
    Domain newInitialValue();

    /**
     * Meet operation for two lattice values.
     */
    Domain meet(Domain v1, Domain v2);

    /**
     * Transfer function for the analysis.
     */
    Domain transfer(Domain in, Node node);

    /**
     * Collect analysis result after reaching fixed point.
     */
    Result getAnalysisResult(Solver<Domain, Result, Node> solver, DirectedGraph<Node> cfg);
}
