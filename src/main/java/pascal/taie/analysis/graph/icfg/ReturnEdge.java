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

import pascal.taie.analysis.dataflow.framework.EdgeTransfer;

import java.util.Objects;

public class ReturnEdge<Node> extends Edge<Node> {

    private final Node callSite;

    public ReturnEdge(Node source, Node target, Node callSite) {
        super(Kind.RETURN, source, target);
        this.callSite = callSite;
    }

    public Node getCallSite() {
        return callSite;
    }

    @Override
    public <Domain> void accept(EdgeTransfer<Node, Domain> transfer,
                                Domain sourceInFlow, Domain sourceOutFlow,
                                Domain edgeFlow) {
        transfer.transferReturnEdge(this, sourceOutFlow, edgeFlow);
    }

    @Override
    protected int computeHashCode() {
        return Objects.hash(kind, source, target, callSite);
    }
}
