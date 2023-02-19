/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2022 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2022 Yue Li <yueli@nju.edu.cn>
 *
 * This file is part of Tai-e.
 *
 * Tai-e is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Tai-e is distributed in the hope that it will be useful,but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Tai-e. If not, see <https://www.gnu.org/licenses/>.
 */

package pascal.taie.analysis.dataflow.analysis;

import pascal.taie.analysis.graph.cfg.CFG;
import pascal.taie.analysis.graph.cfg.CFGEdge;

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
    Fact newBoundaryFact();

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
     * @return true if this analysis needs to perform transfer for given edge, otherwise false.
     */
    boolean needTransferEdge(CFGEdge<Node> edge);

    /**
     * Edge Transfer function for this analysis.
     * Note that this function should NOT modify {@code nodeFact}.
     *
     * @param edge     the edge that the transfer function is applied on
     * @param nodeFact the fact of the source node of the edge. Note that
     *                 which node is the source node of an edge depends on
     *                 the direction of the analysis.
     * @return the resulting edge fact
     */
    Fact transferEdge(CFGEdge<Node> edge, Fact nodeFact);

    /**
     * @return the control-flow graph that this analysis works on.
     */
    CFG<Node> getCFG();

}
