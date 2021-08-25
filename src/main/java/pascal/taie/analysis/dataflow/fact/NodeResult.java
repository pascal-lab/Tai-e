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

package pascal.taie.analysis.dataflow.fact;

/**
 * An interface for querying data-flow results.
 *
 * @param <Node> type of graph nodes
 * @param <Fact> type of data-flow facts
 */
public interface NodeResult<Node, Fact> {

    /**
     * @return the flowing-in fact of given node.
     */
    Fact getInFact(Node node);

    /**
     * @return the flowing-out fact of given node.
     */
    Fact getOutFact(Node node);
}
