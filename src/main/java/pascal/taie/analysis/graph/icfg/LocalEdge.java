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

package pascal.taie.analysis.graph.icfg;

import pascal.taie.analysis.dataflow.analysis.EdgeTransfer;

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
