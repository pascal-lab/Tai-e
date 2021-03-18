/*
 * Tai-e: A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Tai-e is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.taie.ir.exp;

import pascal.taie.language.types.PrimitiveType;
import pascal.taie.language.types.ReferenceType;
import pascal.taie.language.types.Type;

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
    public String toString() {
        return value + " instanceof " + checkedType;
    }
}
