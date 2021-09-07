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

/**
 * Representation of long literals.
 */
public class LongLiteral implements IntegerLiteral {

    /**
     * Cache frequently used literals for saving space.
     */
    private static final LongLiteral[] cache = new LongLiteral[-(-128) + 127 + 1];

    static {
        for (int i = 0; i < cache.length; i++) {
            cache[i] = new LongLiteral(i - 128);
        }
    }

    /**
     * The value of the literal.
     */
    private final long value;

    private LongLiteral(long value) {
        this.value = value;
    }

    public static LongLiteral get(long value) {
        final int offset = 128;
        if (value >= -128 && value <= 127) { // will cache
            return cache[(int) value + offset];
        }
        return new LongLiteral(value);
    }

    @Override
    public PrimitiveType getType() {
        return PrimitiveType.LONG;
    }

    /**
     * @return the value of the literal as a long.
     */
    public long getValue() {
        return value;
    }

    @Override
    public <T> T accept(ExpVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof LongLiteral) {
            return value == ((LongLiteral) o).getValue();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (int) (value ^ (value >>> 32));
    }

    @Override
    public String toString() {
        return value + "L";
    }
}
