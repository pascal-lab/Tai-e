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

package pascal.taie.ir.stmt;

import pascal.taie.ir.exp.LValue;
import pascal.taie.ir.exp.RValue;

import javax.annotation.Nullable;

/**
 * Representation of all definition statements, i.e., exp1 = exp2.
 *
 * @param <L> type of left-hand side expression
 * @param <R> type of right-hand side expression
 */
public abstract class DefinitionStmt<L extends LValue, R extends RValue>
        extends AbstractStmt {

    /**
     * @return the left-hand side expression. If this Stmt is an {@link Invoke}
     * which does not have a left-hand side expression, e.g., o.m(...), then
     * this method returns null; otherwise, it must return a non-null value.
     */
    public abstract @Nullable L getLValue();

    /**
     * @return the right-hand side expression.
     */
    public abstract R getRValue();
}
