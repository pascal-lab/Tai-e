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

import pascal.taie.ir.exp.ArrayAccess;
import pascal.taie.ir.exp.Var;

/**
 * Representation of load array statement, e.g., x = a[..].
 */
public class LoadArray extends ArrayStmt<Var, ArrayAccess> {

    public LoadArray(Var lvalue, ArrayAccess rvalue) {
        super(lvalue, rvalue);
        rvalue.getBase().addLoadArray(this);
    }

    @Override
    public ArrayAccess getArrayAccess() {
        return getRValue();
    }

    @Override
    public <T> T accept(StmtVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
