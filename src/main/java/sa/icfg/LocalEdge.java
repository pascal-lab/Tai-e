package sa.icfg;

import sa.dataflow.analysis.EdgeTransfer;

public class LocalEdge<Node> extends Edge<Node> {

    public LocalEdge(Kind kind, Node source, Node target) {
        super(kind, source, target);
    }

    @Override
    public <Domain> void accept(EdgeTransfer<Node, Domain> transfer,
                       Domain sourceInFlow, Domain sourceOutFlow,
                       Domain edgeFlow) {
        transfer.transferLocalEdge(this, sourceOutFlow, edgeFlow);
    }
}
