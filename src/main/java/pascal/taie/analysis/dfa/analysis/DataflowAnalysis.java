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
     * Create a copy of given fact.
     */
    Fact copyFact(Fact fact);

    /**
     * Merge a fact to another result fact.
     * This function is used to handle control-flow confluences.
     */
    void mergeInto(Fact fact, Fact result);

    /**
     * Node Transfer function for the analysis.
     * The function transfer data-flow from in to out, and return whether
     * the out fact has been changed by the transfer.
     */
    boolean transferNode(Node node, Fact in, Fact out);

    /**
     * Return if this analysis needs to perform edge transfer.
     */
    boolean hasEdgeTransfer();

    /**
     * Edge Transfer function for the analysis.
     * The function transfer data-flow from in to out, and return whether
     * the out fact has been changed by the transfer.
     */
    void transferEdge(Edge<Node> edge, Fact nodeFact, Fact edgeFact);
}
