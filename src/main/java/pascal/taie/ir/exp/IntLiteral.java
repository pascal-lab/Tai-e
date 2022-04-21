/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2022 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2022 Yue Li <yueli@nju.edu.cn>
 *
 * This file is part of Tai-e.
 *
 * Tai-e is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Tai-e is distributed in the hope that it will be useful,but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Tai-e. If not, see <https://www.gnu.org/licenses/>.
 */

package pascal.taie.ir.exp;

import pascal.taie.language.type.PrimitiveType;

/**
 * Representation of int literals.
 */
public class IntLiteral implements IntegerLiteral {

    /**
     * Cache frequently used literals for saving space.
     */
    private static final IntLiteral[] cache = new IntLiteral[-(-128) + 127 + 1];

    static {
        for (int i = 0; i < cache.length; i++) {
            cache[i] = new IntLiteral(i - 128);
        }
    }

    /**
     * The value of the literal.
     */
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

    /**
     * @return the value of the literal as an int.
     */
    public int getValue() {
        return value;
    }

    @Override
    public <T> T accept(ExpVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof IntLiteral) {
            return value == ((IntLiteral) o).getValue();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return value;
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }
}
