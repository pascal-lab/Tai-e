/*
 * Bamboo - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Bamboo is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

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
