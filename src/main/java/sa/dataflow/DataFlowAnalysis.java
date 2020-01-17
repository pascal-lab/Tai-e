package sa.dataflow;

import soot.toolkits.graph.DirectedGraph;

/**
 *
 * @param <Domain> Type for lattice value
 * @param <Result> Type for analysis result
 * @param <Node> Type for nodes of control-flow graph
 */
interface DataFlowAnalysis<Domain, Result, Node> {

    /**
     * Returns whether the analysis is forward.
     */
    boolean isForward();

    /**
     * Returns initial value for entry node.
     */
    Domain getEntryInitialValue(DirectedGraph<Node> cfg);

    /**
     * Returns initial value for other nodes.
     */
    Domain newInitialValue();

    /**
     * Meet operation for lattice values.
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
