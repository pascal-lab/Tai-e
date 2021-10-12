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

package pascal.taie.analysis.dataflow.inter;

import pascal.taie.analysis.graph.icfg.ICFGEdge;

/**
 * Template interface for defining inter-procedural data-flow analysis.
 *
 * @param <Node> type of ICFG nodes
 * @param <Fact> type of data-flow facts
 */
public interface InterDataflowAnalysis<Node, Fact> {

    /**
     * @return true if this analysis is forward, otherwise false.
     */
    boolean isForward();

    /**
     * @return new fact in boundary conditions, i.e., the fact for entry node
     * in forward analysis or exit node in backward analysis.
     */
    Fact newBoundaryFact(Node boundary);

    /**
     * @return new initial fact for non-boundary nodes.
     */
    Fact newInitialFact();

    /**
     * Meets a fact into another (target) fact.
     * This function will be used to handle control-flow confluences.
     */
    void meetInto(Fact fact, Fact target);

    /**
     * Node Transfer function for the analysis.
     * The function transfers data-flow from in (out) fact to out (in) fact
     * for forward (backward) analysis.
     *
     * @return true if the transfer changed the out (int) fact, otherwise false.
     */
    boolean transferNode(Node node, Fact in, Fact out);

    /**
     * Edge Transfer function for this analysis.
     *
     * @param edge the ICFG edge that the transfer function is applied on.
     * @param out  the OUT fact of source node of the edge.
     * @return the result of edge transfer function.
     */
    Fact transferEdge(ICFGEdge<Node> edge, Fact out);
}
