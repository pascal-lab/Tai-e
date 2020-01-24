package sa.icfg;

import sa.dataflow.analysis.EdgeTransfer;

public class CallEdge<Node> extends Edge<Node> {

    public CallEdge(Kind kind, Node source, Node target) {
        super(kind, source, target);
    }

    @Override
    public <Domain> void accept(EdgeTransfer<Node, Domain> transfer,
                       Domain sourceInFlow, Domain sourceOutFlow,
                       Domain edgeFlow) {
        transfer.transferCallEdge(this, sourceInFlow, edgeFlow);
    }
}
