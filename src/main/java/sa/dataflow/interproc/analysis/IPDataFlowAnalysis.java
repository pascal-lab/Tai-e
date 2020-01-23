package sa.dataflow.interproc.analysis;

import sa.dataflow.analysis.DataFlowAnalysis;

/**
 * Inter-procedural data-flow analysis problem.
 * @param <Domain>
 * @param <Method>
 * @param <Node>
 */
public interface IPDataFlowAnalysis<Domain, Method, Node>
        extends DataFlowAnalysis<Domain, Node> {

    boolean transferFunctionEntry(Node callSite, Domain callSiteIn,
                         Method callee, Node entry, Domain entryIn);

    boolean transferFunctionExit(Method callee, Node exit, Domain returnOut,
                           Node CallSite, Domain callSiteOut);

    boolean transferCallToReturn(Node callSite, Domain in, Domain out);
}
