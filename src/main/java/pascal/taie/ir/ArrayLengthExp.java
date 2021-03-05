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

package pascal.taie.ir;

import pascal.taie.java.types.ArrayType;
import pascal.taie.java.types.PrimitiveType;

/**
 * Representation of array length expression, e.g., arr.length.
 */
public class ArrayLengthExp implements UnaryExp {

    private final Var base;

    public ArrayLengthExp(Var base) {
        this.base = base;
        assert base.getType() instanceof ArrayType;
    }

    public Var getBase() {
        return base;
    }

    @Override
    public PrimitiveType getType() {
        return PrimitiveType.INT;
    }

    @Override
    public String toString() {
        return base + ".length";
    }
}
