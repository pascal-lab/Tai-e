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

public class IntLiteral implements IntegerLiteral {

    /**
     * Cache frequently used literals for saving space.
     */
    private static final IntLiteral[] cache = new IntLiteral[-(-128) + 127 + 1];

    static {
        for(int i = 0; i < cache.length; i++)
            cache[i] = new IntLiteral(i - 128);
    }

    private final int value;

    private IntLiteral(int value) {
        this.value = value;
    }

    public static IntLiteral get(int value) {
        final int offset = 128;
        if (value >= -128 && value <= 127) { // will cache
            return cache[value + offset];
        }
        return new IntLiteral(value);
    }

    @Override
    public PrimitiveType getType() {
        return PrimitiveType.INT;
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }
}
