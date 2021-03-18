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

import pascal.taie.language.types.NullType;

public enum NullLiteral implements ReferenceLiteral<Void> {

    INSTANCE;

    public static NullLiteral get() {
        return INSTANCE;
    }

    @Override
    public NullType getType() {
        return NullType.NULL;
    }

    @Override
    public Void getValue() {
        return null;
    }

    @Override
    public String toString() {
        return "null";
    }
}
