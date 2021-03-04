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

import pascal.taie.java.World;
import pascal.taie.java.types.PrimitiveType;

public class FloatLiteral implements FloatingPointLiteral {

    private static final PrimitiveType type = World.get()
            .getTypeManager()
            .getFloatType();

    /**
     * Cache frequently used literals for saving space.
     */
    private static final FloatLiteral ZERO = new FloatLiteral(0);

    private final float value;

    private FloatLiteral(float value) {
        this.value = value;
    }

    public static FloatLiteral get(float value) {
        return value == 0 ? ZERO : new FloatLiteral(value);
    }

    @Override
    public PrimitiveType getType() {
        return type;
    }

    public float getValue() {
        return value;
    }
}
