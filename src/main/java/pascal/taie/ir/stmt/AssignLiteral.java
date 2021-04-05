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

import pascal.taie.ir.exp.Literal;
import pascal.taie.ir.exp.Var;

/**
 * Representation of statement that assigns literals, e.g., a = 10.
 * TODO: give a better name (replace Assign)?
 */
public class AssignLiteral extends AssignStmt<Var, Literal> {

    public AssignLiteral(Var lvalue, Literal rvalue) {
        super(lvalue, rvalue);
    }

    @Override
    public void accept(StmtVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public <T> T accept(StmtRVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
