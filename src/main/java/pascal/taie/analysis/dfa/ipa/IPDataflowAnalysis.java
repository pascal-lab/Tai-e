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

/**
 * Inter-procedural data-flow analysis.
 */
public interface IPDataflowAnalysis<Method, Node, Fact> {

    /**
     * Returns whether the analysis is forward.
     */
    boolean isForward();

    /**
     * Returns initial flowing-in fact for entry node.
     */
    Fact getEntryInitialFact(ICFG<Method, Node> icfg);

    /**
     * Returns initial flowing-out fact for non-entry nodes.
     */
    Fact newInitialFact();

    /**
     * Creates a copy of given fact.
     */
    Fact copyFact(Fact fact);

    /**
     * Merges a fact to another result fact.
     * This function is used to handle control-flow confluences.
     */
    void mergeInto(Fact fact, Fact result);

    /**
     * Node transfer function for non-call nodes.
     */
    boolean transferNonCall(Node node, Fact in, Fact out);

    /**
     * Node transfer function for call nodes.
     */
    boolean transferCall(Node callSite, Fact in, Fact out);

    default void transferEdge(
            ICFGEdge<Node> edge, Fact in, Fact out, Fact edgeFact) {
        if (edge instanceof LocalEdge) {
            transferLocalEdge((LocalEdge<Node>) edge, out, edgeFact);
        } else if (edge instanceof CallEdge) {
            transferCallEdge((CallEdge<Node>) edge, in, edgeFact);
        } else {
            transferReturnEdge((ReturnEdge<Node>) edge, out, edgeFact);
        }
    }

    void transferLocalEdge(LocalEdge<Node> edge, Fact out, Fact edgeFact);

    void transferCallEdge(CallEdge<Node> edge, Fact callSiteIn, Fact edgeFact);

    void transferReturnEdge(ReturnEdge<Node> edge, Fact returnOut, Fact edgeFact);
}
