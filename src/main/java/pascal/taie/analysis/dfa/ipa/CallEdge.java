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

package pascal.taie.analysis.dfa.ipa;

import pascal.taie.analysis.dataflow.framework.EdgeTransfer;

public class CallEdge<Node> extends ICFGEdge<Node> {

    CallEdge(Node source, Node target) {
        super(Kind.CALL, source, target);
    }

    @Override
    public <Domain> void accept(EdgeTransfer<Node, Domain> transfer,
                                Domain sourceInFlow, Domain sourceOutFlow,
                                Domain edgeFlow) {
//        transfer.transferCallEdge(this, sourceInFlow, edgeFlow);
    }
}
