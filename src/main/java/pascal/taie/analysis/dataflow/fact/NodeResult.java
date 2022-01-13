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

import pascal.taie.analysis.StmtResult;
import pascal.taie.ir.stmt.Stmt;

/**
 * An interface for querying data-flow results.
 *
 * @param <Node> type of graph nodes
 * @param <Fact> type of data-flow facts
 */
public interface NodeResult<Node, Fact> extends StmtResult<Fact> {

    /**
     * @return the flowing-in fact of given node.
     */
    Fact getInFact(Node node);

    /**
     * @return the flowing-out fact of given node.
     */
    Fact getOutFact(Node node);

    /**
     * Typically, all {@code stmt}s are relevant in {@code NodeResult}.
     *
     * @return {@code true}.
     */
    @Override
    default boolean isRelevant(Stmt stmt) {
        return true;
    }

    /**
     * {@link NodeResult} is designed to be compatible with CFGs of both
     * stmt nodes and block nodes. When the node result instance represent
     * results of stmt nodes, it can be used as a {@link StmtResult}.
     *
     * @return out fact as the analysis result for given stmt.
     */
    @Override
    default Fact getResult(Stmt stmt) {
        return getOutFact((Node) stmt);
    }
}
