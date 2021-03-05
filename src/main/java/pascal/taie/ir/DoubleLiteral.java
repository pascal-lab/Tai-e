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

public class DoubleLiteral implements FloatingPointLiteral {

    /**
     * Cache frequently used literals for saving space.
     */
    private static final DoubleLiteral ZERO = new DoubleLiteral(0);

    private final double value;

    private DoubleLiteral(double value) {
        this.value = value;
    }

    public static DoubleLiteral get(double value) {
        return value == 0 ? ZERO : new DoubleLiteral(value);
    }

    @Override
    public PrimitiveType getType() {
        return PrimitiveType.DOUBLE;
    }

    public double getValue() {
        return value;
    }

    @Override
    public String toString() {
        return Double.toString(value);
    }
}
