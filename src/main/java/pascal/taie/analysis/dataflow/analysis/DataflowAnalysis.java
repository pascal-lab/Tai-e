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

/**
 * Template interface for defining data-flow analysis.
 *
 * @param <Node> type of CFG nodes
 * @param <Fact> type of data-flow facts
 */
public interface DataflowAnalysis<Node, Fact> {

    /**
     * @return true if this analysis is forward, otherwise false.
     */
    boolean isForward();

    /**
     * @return new fact in boundary conditions, i.e., the fact for
     * entry (exit) node in forward (backward) analysis.
     */
    Fact newBoundaryFact(CFG<Node> cfg);

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
     * @return true if the transfer changed the out (in) fact, otherwise false.
     */
    boolean transferNode(Node node, Fact in, Fact out);

    /**
     * @return true if this analysis needs to perform edge transfer, otherwise false.
     */
    boolean hasEdgeTransfer();

    /**
     * @return true if the given edge needs to be applied transfer function,
     * otherwise false.
     */
    boolean needTransfer(Edge<Node> edge);

    /**
     * Edge Transfer function for this analysis.
     */
    void transferEdge(Edge<Node> edge, Fact nodeFact, Fact edgeFact);
}
