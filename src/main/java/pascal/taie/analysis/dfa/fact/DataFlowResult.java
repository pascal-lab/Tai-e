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

package pascal.taie.analysis.dfa.fact;

import pascal.taie.analysis.graph.cfg.Edge;

public interface DataFlowResult<Node, Fact> {

    /**
     * @return the flowing-in fact of given node.
     */
    Fact getInFact(Node node);

    void setInFact(Node node, Fact fact);

    /**
     * @return the flowing-out fact of given node.
     */
    Fact getOutFact(Node node);

    void setOutFact(Node node, Fact fact);

    /**
     * @return the fact of given edge.
     */
    Fact getEdgeFact(Edge<Node> edge);

    void setEdgeFact(Edge<Node> edge, Fact fact);
}
