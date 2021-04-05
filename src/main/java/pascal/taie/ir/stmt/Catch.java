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

/**
 * Representation of catch exception, e.g., catch (e).
 */
public class Catch extends AbstractStmt {

    /**
     * Reference of the exception object to be caught.
     */
    private final Var exceptionRef;

    public Catch(Var exceptionRef) {
        this.exceptionRef = exceptionRef;
    }

    public Var getExceptionRef() {
        return exceptionRef;
    }

    @Override
    public boolean canFallThrough() {
        return true;
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
        return "catch " + exceptionRef;
    }
}
