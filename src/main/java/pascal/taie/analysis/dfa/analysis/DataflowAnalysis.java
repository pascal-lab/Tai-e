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

package pascal.taie.analysis.dfa.analysis;

import pascal.taie.analysis.graph.cfg.Edge;

public interface DataflowAnalysis<Node, Fact> {

    /**
     * Returns whether the analysis is forward.
     */
    boolean isForward();

    /**
     * Returns initial flowing-in fact for entry node.
     */
    Fact getEntryInitialFact();

    /**
     * Returns initial flowing-out fact for non-entry nodes.
     */
    Fact newInitialFact();

    /**
     * Meet function for two data-flow facts.
     * This function is used to handle control-flow confluences.
     */
    Fact meet(Fact f1, Fact f2);

    /**
     * Node Transfer function for the analysis.
     * The function transfer data-flow from in to out, and return whether
     * the out fact has been changed by the transfer.
     */
    boolean transferNode(Node node, Fact in, Fact out);

    /**
     * Return if this analysis needs to perform edge transfer.
     */
    default boolean hasEdgeTransfer() {
        return false;
    }

    /**
     * Edge Transfer function for the analysis.
     * The function transfer data-flow from in to out, and return whether
     * the out fact has been changed by the transfer.
     */
    default boolean transferEdge(Edge<Node> edge, Fact in, Fact out) {
        return false;
    }
}
