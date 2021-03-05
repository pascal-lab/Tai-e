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

import pascal.taie.java.types.PrimitiveType;
import pascal.taie.java.types.Type;

/**
 * Representation of instanceof expression, e.g., o instanceof T.
 */
public class InstanceOfExp implements Exp {

    /**
     * The value to be checked.
     */
    private final Atom value;

    private final Type checkedType;

    public InstanceOfExp(Atom value, Type checkedType) {
        this.value = value;
        this.checkedType = checkedType;
    }

    public Atom getValue() {
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
