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

import pascal.taie.java.types.Type;

/**
 * Representation of cast expression, e.g., (T) o.
 */
public class CastExp implements Exp {

    /**
     * The value to be casted.
     */
    private final Atom value;

    private final Type castType;

    public CastExp(Atom value, Type castType) {
        this.value = value;
        this.castType = castType;
    }

    public Atom getValue() {
        return value;
    }

    public Type getCastType() {
        return castType;
    }

    @Override
    public Type getType() {
        return castType;
    }

    @Override
    public String toString() {
        return String.format("(%s) %s", castType, value);
    }
}
