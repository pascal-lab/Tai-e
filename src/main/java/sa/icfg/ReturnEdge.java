package sa.icfg;

import sa.dataflow.analysis.EdgeTransfer;

import java.util.Objects;

public class ReturnEdge<Node> extends Edge<Node> {

    private final Node callSite;

    public ReturnEdge(Node source, Node target, Node callSite) {
        super(Kind.RETURN, source, target);
        this.callSite = callSite;
    }

    public Node getCallSite() {
        return callSite;
    }

    @Override
    public <Domain> void accept(EdgeTransfer<Node, Domain> transfer,
                       Domain sourceInFlow, Domain sourceOutFlow,
                       Domain edgeFlow) {
        transfer.transferReturnEdge(this, sourceOutFlow, edgeFlow);
    }

    @Override
    protected int computeHashCode() {
        return Objects.hash(kind, source, target, callSite);
    }
}
