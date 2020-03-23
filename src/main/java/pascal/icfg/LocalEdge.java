package pascal.icfg;

import pascal.dataflow.analysis.EdgeTransfer;

public class LocalEdge<Node> extends Edge<Node> {

    public LocalEdge(Node source, Node target) {
        super(Kind.LOCAL, source, target);
    }

    @Override
    public <Domain> void accept(EdgeTransfer<Node, Domain> transfer,
                       Domain sourceInFlow, Domain sourceOutFlow,
                       Domain edgeFlow) {
        transfer.transferLocalEdge(this, sourceOutFlow, edgeFlow);
    }
}
