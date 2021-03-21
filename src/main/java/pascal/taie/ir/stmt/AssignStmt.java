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

/**
 * Representation of assign statements.
 * @param <L> type of lvalue.
 * @param <R> type of rvalue.
 */
public abstract class AssignStmt<L extends LValue, R extends RValue> extends AbstractStmt {

    private final L lvalue;

    private final R rvalue;

    public AssignStmt(L lvalue, R rvalue) {
        this.lvalue = lvalue;
        this.rvalue = rvalue;
    }

    public L getLValue() {
        return lvalue;
    }

    public R getRValue() {
        return rvalue;
    }

    @Override
    public boolean canFallThrough() {
        return true;
    }

    @Override
    public String toString() {
        return lvalue + " = " + rvalue;
    }
}
