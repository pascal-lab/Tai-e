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

package pascal.taie.analysis.dataflow.analysis;

import pascal.taie.analysis.graph.cfg.CFG;
import pascal.taie.analysis.graph.cfg.Edge;

public interface DataflowAnalysis<Node, Fact> {

    /**
     * Returns whether the analysis is forward.
     */
    boolean isForward();

    /**
     * Returns new fact in boundary conditions, i.e., the fact for entry node
     * in forward analysis or exit node in backward analysis.
     */
    Fact newBoundaryFact(CFG<Node> cfg);

    /**
     * Returns new initial fact for non-boundary nodes.
     */
    Fact newInitialFact();

    /**
     * Merges a fact to another result fact.
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
     * Returns if this analysis needs to perform edge transfer.
     */
    boolean hasEdgeTransfer();

    /**
     * @return if given edge needs to be applied transfer function.
     */
    boolean needTransfer(Edge<Node> edge);

    /**
     * Edge Transfer function for the analysis.
     * The function transfer data-flow from in to out, and return whether
     * the out fact has been changed by the transfer.
     */
    void transferEdge(Edge<Node> edge, Fact nodeFact, Fact edgeFact);
}
