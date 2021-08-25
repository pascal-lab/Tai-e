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

import pascal.taie.ir.exp.FieldAccess;
import pascal.taie.ir.exp.InstanceFieldAccess;
import pascal.taie.ir.exp.Var;

/**
 * Representation of following store field statements:
 * - store instance field: o.f = x
 * - store static field: T.f = x
 */
public class StoreField extends FieldStmt<FieldAccess, Var> {

    public StoreField(FieldAccess lvalue, Var rvalue) {
        super(lvalue, rvalue);
        if (lvalue instanceof InstanceFieldAccess) {
            Var base = ((InstanceFieldAccess) lvalue).getBase();
            base.addStoreField(this);
        }
    }

    @Override
    public FieldAccess getFieldAccess() {
        return getLValue();
    }

    @Override
    public <T> T accept(StmtVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
