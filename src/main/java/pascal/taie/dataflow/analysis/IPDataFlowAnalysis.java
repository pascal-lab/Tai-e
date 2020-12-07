/*
 * Panda - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Panda is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.taie.dataflow.analysis;

import pascal.taie.icfg.Edge;

/**
 * Inter-procedural data-flow analysis problem.
 *
 * @param <Domain>
 * @param <Method>
 * @param <Node>
 */
public interface IPDataFlowAnalysis<Domain, Method, Node>
        extends DataFlowAnalysis<Domain, Node> {

    boolean transferCallNode(Node callSite, Domain in, Domain out);

    default void transferEdge(Edge<Node> edge,
                              Domain sourceInFlow, Domain sourceOutFlow,
                              Domain edgeFlow) {
        edge.accept(getEdgeTransfer(), sourceInFlow, sourceOutFlow, edgeFlow);
    }

    EdgeTransfer<Node, Domain> getEdgeTransfer();
}
