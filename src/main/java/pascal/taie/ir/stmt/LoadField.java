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
 * Representation of following load field statements:
 * - load instance field: x = o.f
 * - load static field: x = T.f
 */
public class LoadField extends FieldStmt<Var, FieldAccess> {

    public LoadField(Var lvalue, FieldAccess rvalue) {
        super(lvalue, rvalue);
        if (rvalue instanceof InstanceFieldAccess) {
            Var base = ((InstanceFieldAccess) rvalue).getBase();
            base.addLoadField(this);
        }
    }

    @Override
    public FieldAccess getFieldAccess() {
        return getRValue();
    }

    @Override
    public <T> T accept(StmtVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
