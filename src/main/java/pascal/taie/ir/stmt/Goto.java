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

/**
 * Representation of goto statement, e.g., goto L.
 */
public class Goto extends JumpStmt {

    private Stmt target;

    public Stmt getTarget() {
        return target;
    }

    public void setTarget(Stmt target) {
        this.target = target;
    }

    @Override
    public void accept(StmtVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return "goto " + toString(target);
    }
}
