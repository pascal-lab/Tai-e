package sa.dataflow.analysis;

import sa.dataflow.analysis.DataFlowAnalysis;

/**
 * Inter-procedural data-flow analysis problem.
 * @param <Domain>
 * @param <Method>
 * @param <Node>
 */
public interface IPDataFlowAnalysis<Domain, Method, Node>
        extends DataFlowAnalysis<Domain, Node> {

    boolean transferCallNode(Node callSite, Domain in, Domain out);

    boolean transferCallEdge(Node callSite, Domain callSiteIn,
                                  Method callee, Node entry, Domain entryFlow);

    boolean transferReturnEdge(Method callee, Node exit, Domain exitInFlow,
                                 Domain exitFlow, Node callSite, Node returnSite);
}
