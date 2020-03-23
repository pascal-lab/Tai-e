package pascal.icfg;

import pascal.dataflow.analysis.EdgeTransfer;

public class CallEdge<Node> extends Edge<Node> {

    public CallEdge(Node source, Node target) {
        super(Kind.CALL, source, target);
    }

    @Override
    public <Domain> void accept(EdgeTransfer<Node, Domain> transfer,
                       Domain sourceInFlow, Domain sourceOutFlow,
                       Domain edgeFlow) {
        transfer.transferCallEdge(this, sourceInFlow, edgeFlow);
    }
}
