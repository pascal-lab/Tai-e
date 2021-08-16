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

package pascal.taie.analysis.dataflow.ipa;

import pascal.taie.analysis.graph.icfg.ICFGEdge;

/**
 * Inter-procedural data-flow analysis.
 */
public interface IPDataflowAnalysis<Method, Node, Fact> {

    /**
     * Returns whether the analysis is forward.
     */
    boolean isForward();

    /**
     * Returns new fact in boundary conditions, i.e., the fact for entry node
     * in forward analysis or exit node in backward analysis.
     */
    Fact newBoundaryFact(Node boundary);

    /**
     * Returns new initial fact for non-boundary nodes.
     */
    Fact newInitialFact();

    /**
     * Merges a fact to another result fact.
     * This function is used to handle control-flow confluences.
     */
    void mergeInto(Fact fact, Fact result);

    boolean transferNode(Node node, Fact in, Fact out);

    void transferEdge(ICFGEdge<Node> edge, Fact in, Fact out, Fact edgeFact);
}
