package sa.dataflow.analysis;

import sa.icfg.Edge;

/**
 * Inter-procedural data-flow analysis problem.
 * @param <Domain>
 * @param <Method>
 * @param <Node>
 */
public interface IPDataFlowAnalysis<Domain, Method, Node>
        extends DataFlowAnalysis<Domain, Node> {

    boolean transferCallNode(Node callSite, Domain in, Domain out);

    default void transferEdge(Edge<Node> edge,
                              Domain sourceInFlow, Domain sourceOutFlow,
                              Domain edgeFlow) {
        edge.accept(getEdgeTransfer(), sourceInFlow, sourceOutFlow, edgeFlow);
    }

    EdgeTransfer<Node, Domain> getEdgeTransfer();
}
