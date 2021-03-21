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

import pascal.taie.ir.exp.Var;

import javax.annotation.Nullable;

/**
 * Representation of return statement, e.g., return; or return x.
 */
public class Return extends AbstractStmt {

    private final Var value;

    public Return(Var value) {
        this.value = value;
    }

    public Return() {
        this(null);
    }

    public @Nullable Var getValue() {
        return value;
    }

    @Override
    public boolean canFallThrough() {
        return false;
    }

    @Override
    public void accept(StmtVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return value != null ? "return " + value : "return";
    }
}
