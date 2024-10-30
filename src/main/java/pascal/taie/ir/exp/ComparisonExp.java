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
import pascal.taie.language.type.FloatType;
import pascal.taie.language.type.IntType;
import pascal.taie.language.type.LongType;
import pascal.taie.language.type.Type;

/**
 * Representation of comparison expression, e.g., cmp.
 */
public class ComparisonExp extends AbstractBinaryExp {

    public enum Op implements BinaryExp.Op {

        CMP("cmp"),
        CMPL("cmpl"),
        CMPG("cmpg"),
        ;

        private final String instruction;

        Op(String instruction) {
            this.instruction = instruction;
        }

        @Override
        public String toString() {
            return instruction;
        }
    }

    private final Op op;

    public ComparisonExp(Op op, Var value1, Var value2) {
        super(value1, value2);
        this.op = op;
    }

    @Override
    protected void validate() {
        Type v1type = operand1.getType();
        assert v1type.equals(operand2.getType());
        assert v1type.equals(LongType.LONG) ||
                v1type.equals(FloatType.FLOAT) ||
                v1type.equals(DoubleType.DOUBLE);
    }

    @Override
    public Op getOperator() {
        return op;
    }

    @Override
    public IntType getType() {
        return IntType.INT;
    }

    @Override
    public <T> T accept(ExpVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
