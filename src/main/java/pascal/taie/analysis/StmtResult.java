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

package pascal.taie.analysis;

import pascal.taie.ir.stmt.Stmt;

/**
 * An interface for querying analysis results of Stmt.
 *
 * @param <R> type of analysis results
 */
public interface StmtResult<R> {

    /**
     * @return if {@code stmt} is relevant in this result.
     */
    boolean isRelevant(Stmt stmt);

    /**
     * @return analysis result of given stmt.
     */
    R getResult(Stmt stmt);
}
