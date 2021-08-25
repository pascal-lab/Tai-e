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
 * Representation of store array statement, e.g., a[..] = x.
 */
public class StoreArray extends ArrayStmt<ArrayAccess, Var> {

    public StoreArray(ArrayAccess lvalue, Var rvalue) {
        super(lvalue, rvalue);
        lvalue.getBase().addStoreArray(this);
    }

    @Override
    public ArrayAccess getArrayAccess() {
        return getLValue();
    }

    @Override
    public <T> T accept(StmtVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
