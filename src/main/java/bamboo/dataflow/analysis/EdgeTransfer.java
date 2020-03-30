package bamboo.dataflow.analysis;

import bamboo.icfg.CallEdge;
import bamboo.icfg.LocalEdge;
import bamboo.icfg.ReturnEdge;

public interface EdgeTransfer<Node, Domain> {

    void transferLocalEdge(LocalEdge<Node> edge, Domain nodeOut, Domain edgeFlow);

    void transferCallEdge(CallEdge<Node> edge,
                          Domain callSiteInFlow, Domain edgeFlow);

    void transferReturnEdge(ReturnEdge<Node> edge,
                            Domain returnOutFlow, Domain edgeFlow);
}
