/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2020-- Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020-- Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * Tai-e is only for educational and academic purposes,
 * and any form of commercial use is disallowed.
 * Distribution of Tai-e is disallowed without the approval.
 */

package pascal.taie.analysis.dataflow.framework;

import pascal.taie.analysis.graph.icfg.CallEdge;
import pascal.taie.analysis.graph.icfg.LocalEdge;
import pascal.taie.analysis.graph.icfg.ReturnEdge;

public interface EdgeTransfer<Node, Domain> {

    void transferLocalEdge(LocalEdge<Node> edge, Domain nodeOut, Domain edgeFlow);

    void transferCallEdge(CallEdge<Node> edge,
                          Domain callSiteInFlow, Domain edgeFlow);

    void transferReturnEdge(ReturnEdge<Node> edge,
                            Domain returnOutFlow, Domain edgeFlow);
}
