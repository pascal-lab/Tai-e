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

import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.Type;

import java.util.List;

/**
 * Representation of array access expression, e.g., a[i].
 */
public class ArrayAccess implements LValue, RValue {

    private final Var base;

    private final Var index;

    public ArrayAccess(Var base, Var index) {
        this.base = base;
        this.index = index;
        assert base.getType() instanceof ArrayType;
    }

    public Var getBase() {
        return base;
    }

    public Var getIndex() {
        return index;
    }

    @Override
    public Type getType() {
        if (base.getType() instanceof ArrayType) {
            return ((ArrayType) base.getType()).getElementType();
        } else {
            throw new RuntimeException("Invalid base type: " + base.getType());
        }
    }

    @Override
    public List<RValue> getUses() {
        return List.of(base, index);
    }

    @Override
    public <T> T accept(ExpVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return String.format("%s[%s]", base, index);
    }
}
