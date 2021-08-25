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

package pascal.taie.ir.exp;

import pascal.taie.ir.proginfo.FieldRef;

import java.util.List;

/**
 * Representation of instance field access expression, e.g., o.f.
 */
public class InstanceFieldAccess extends FieldAccess {

    private final Var base;

    public InstanceFieldAccess(FieldRef fieldRef, Var base) {
        super(fieldRef);
        this.base = base;
    }

    public Var getBase() {
        return base;
    }

    @Override
    public List<RValue> getUses() {
        return List.of(base);
    }

    @Override
    public <T> T accept(ExpVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return base + "." + fieldRef;
    }
}
