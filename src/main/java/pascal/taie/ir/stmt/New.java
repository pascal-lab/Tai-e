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

import pascal.taie.ir.exp.NewExp;
import pascal.taie.ir.exp.Var;
import pascal.taie.language.classes.JMethod;

/**
 * Representation of following kinds of new statements:
 * - new instance: o = new T
 * - new array: o = new T[..]
 * - new multi-array: o = new T[..][..]
 */
public class New extends AssignStmt<Var, NewExp> {

    /**
     * The method containing this new statement.
     */
    private final JMethod container;

    public New(JMethod method, Var lvalue, NewExp rvalue) {
        super(lvalue, rvalue);
        this.container = method;
    }

    public JMethod getContainer() {
        return container;
    }

    @Override
    public <T> T accept(StmtVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
