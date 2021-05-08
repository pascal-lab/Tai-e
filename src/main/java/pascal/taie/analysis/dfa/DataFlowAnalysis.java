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

package pascal.taie.analysis.dfa;

import pascal.taie.analysis.graph.cfg.Edge;

public interface DataFlowAnalysis<Node, Flow> {

    /**
     * Returns whether the analysis is forward.
     */
    boolean isForward();

    /**
     * Returns initial in-flow for entry node.
     */
    Flow getEntryInitialFlow();

    /**
     * Returns initial out-flow value for non-entry nodes.
     */
    Flow newInitialFlow();

    /**
     * Transfer function for the analysis.
     * The function transfer data-flow from in to out, and return whether
     * the out flow has been changed by the transfer.
     */
    boolean transfer(Node node, Flow in, Flow out);

    /**
     * Meets out flow of predecessor node to in flow of successor node.
     * This is also the place to perform edge transfer, if necessary.
     */
    void meetInto(Flow predOut, Edge<Node> edge, Flow succIn);
}
