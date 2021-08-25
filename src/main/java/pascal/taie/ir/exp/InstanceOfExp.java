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

import pascal.taie.language.type.PrimitiveType;
import pascal.taie.language.type.ReferenceType;
import pascal.taie.language.type.Type;

import java.util.List;

/**
 * Representation of instanceof expression, e.g., o instanceof T.
 */
public class InstanceOfExp implements RValue {

    /**
     * The value to be checked.
     */
    private final Var value;

    private final Type checkedType;

    public InstanceOfExp(Var value, Type checkedType) {
        this.value = value;
        this.checkedType = checkedType;
        assert checkedType instanceof ReferenceType;
    }

    public Var getValue() {
        return value;
    }

    public Type getCheckedType() {
        return checkedType;
    }

    @Override
    public PrimitiveType getType() {
        return PrimitiveType.BOOLEAN;
    }

    @Override
    public List<RValue> getUses() {
        return List.of(value);
    }

    @Override
    public <T> T accept(ExpVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return value + " instanceof " + checkedType;
    }
}
