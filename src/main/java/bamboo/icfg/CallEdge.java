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

package bamboo.icfg;

import bamboo.dataflow.analysis.EdgeTransfer;

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
