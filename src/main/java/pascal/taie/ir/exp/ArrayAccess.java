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

import pascal.taie.java.types.ArrayType;
import pascal.taie.java.types.Type;

/**
 * Representation of array access expression, e.g., a[i].
 */
public class ArrayAccess implements Exp {

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
    public String toString() {
        return String.format("%s[%s]", base, index);
    }
}
