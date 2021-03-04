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

/**
 * Representation of new array expression, e.g., new T[..].
 */
public class NewArray extends NewExp {

    private final ArrayType type;

    private final Atom length;

    public NewArray(ArrayType type, Atom length) {
        this.type = type;
        this.length = length;
    }

    @Override
    public ArrayType getType() {
        return type;
    }

    public Atom getLength() {
        return length;
    }

    @Override
    public String toString() {
        return String.format("new %s[%s]", type.getElementType(), length);
    }
}
