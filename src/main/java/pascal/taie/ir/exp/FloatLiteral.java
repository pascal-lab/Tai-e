/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2020-- Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020-- Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * Tai-e is only for educational and academic purposes,
 * and any form of commercial use is disallowed.
 * Distribution of Tai-e is disallowed without the approval.
 */

package pascal.taie.ir.exp;

import pascal.taie.language.type.PrimitiveType;

public class FloatLiteral implements FloatingPointLiteral {

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
        return PrimitiveType.FLOAT;
    }

    public float getValue() {
        return value;
    }

    @Override
    public <T> T accept(ExpVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return value + "F";
    }
}
