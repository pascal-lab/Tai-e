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

import pascal.taie.language.type.DoubleType;

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
    public DoubleType getType() {
        return DoubleType.DOUBLE;
    }

    public double getValue() {
        return value;
    }

    @Override
    public Double getNumber() {
        return value;
    }

    @Override
    public <T> T accept(ExpVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DoubleLiteral that = (DoubleLiteral) o;
        return Double.compare(that.value, value) == 0;
    }

    @Override
    public int hashCode() {
        long temp = Double.doubleToLongBits(value);
        return (int) (temp ^ (temp >>> 32));
    }

    @Override
    public String toString() {
        return Double.toString(value);
    }
}
