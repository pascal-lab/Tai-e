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

import pascal.taie.ir.exp.RValue;
import pascal.taie.ir.exp.Var;

import java.util.List;

/**
 * Representation of throw exception statement, e.g., throw e.
 */
public class Throw extends AbstractStmt {

    /**
     * Reference of the exception object to be thrown.
     */
    private final Var exceptionRef;

    public Throw(Var exceptionRef) {
        this.exceptionRef = exceptionRef;
    }

    public Var getExceptionRef() {
        return exceptionRef;
    }

    @Override
    public List<RValue> getUses() {
        return List.of(exceptionRef);
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
    public <T> T accept(StmtRVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return "throw " + exceptionRef;
    }
}
