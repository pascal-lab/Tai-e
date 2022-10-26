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

package pascal.taie.analysis.dataflow.analysis.constprop;

import pascal.taie.ir.exp.ArithmeticExp;
import pascal.taie.ir.exp.BinaryExp;
import pascal.taie.ir.exp.BitwiseExp;
import pascal.taie.ir.exp.ConditionExp;
import pascal.taie.ir.exp.Exp;
import pascal.taie.ir.exp.Exps;
import pascal.taie.ir.exp.IntLiteral;
import pascal.taie.ir.exp.ShiftExp;
import pascal.taie.ir.exp.Var;
import pascal.taie.util.AnalysisException;

/**
 * Evaluates expressions in constant propagation. Since this functionality
 * is used not only by {@link ConstantPropagation} but also other classes,
 * we implement it as static methods to make it easily accessible.
 */
public final class Evaluator {

    private Evaluator() {
    }

    /**
     * Evaluates the {@link Value} of given expression.
     *
     * @param exp the expression to be evaluated
     * @param in  IN fact of the statement
     * @return the resulting {@link Value}
     */
    public static Value evaluate(Exp exp, CPFact in) {
        if (exp instanceof IntLiteral) {
            return Value.makeConstant(((IntLiteral) exp).getValue());
        } else if (exp instanceof Var var) {
            // treat the values of non-int variables as NAC
            return Exps.holdsInt(var) ? in.get(var) : Value.getNAC();
        } else if (exp instanceof BinaryExp binary) {
            BinaryExp.Op op = binary.getOperator();
            Value v1 = evaluate(binary.getOperand1(), in);
            Value v2 = evaluate(binary.getOperand2(), in);
            // handle division-by-zero by returning UNDEF
            if ((op == ArithmeticExp.Op.DIV || op == ArithmeticExp.Op.REM) &&
                    v2.isConstant() && v2.getConstant() == 0) {
                return Value.getUndef();
            }
            if (v1.isConstant() && v2.isConstant()) {
                int i1 = v1.getConstant();
                int i2 = v2.getConstant();
                return Value.makeConstant(evaluate(op, i1, i2));
            }
            // handle zero * NAC by returning 0
            if (op == ArithmeticExp.Op.MUL
                    && (v1.isConstant() && v1.getConstant() == 0 && v2.isNAC() || // 0 * NAC
                    v2.isConstant() && v2.getConstant() == 0 && v1.isNAC())) { // NAC * 0
                return Value.makeConstant(0);
            }
            if (v1.isNAC() || v2.isNAC()) {
                return Value.getNAC();
            }
            return Value.getUndef();
        }
        // return NAC for other cases
        return Value.getNAC();
    }

    private static int evaluate(BinaryExp.Op op, int i1, int i2) {
        if (op instanceof ArithmeticExp.Op) {
            return switch ((ArithmeticExp.Op) op) {
                case ADD -> i1 + i2;
                case SUB -> i1 - i2;
                case MUL -> i1 * i2;
                case DIV -> i1 / i2;
                case REM -> i1 % i2;
            };
        } else if (op instanceof BitwiseExp.Op) {
            return switch ((BitwiseExp.Op) op) {
                case OR -> i1 | i2;
                case AND -> i1 & i2;
                case XOR -> i1 ^ i2;
            };
        } else if (op instanceof ConditionExp.Op) {
            return switch ((ConditionExp.Op) op) {
                case EQ -> i1 == i2 ? 1 : 0;
                case NE -> i1 != i2 ? 1 : 0;
                case LT -> i1 < i2 ? 1 : 0;
                case GT -> i1 > i2 ? 1 : 0;
                case LE -> i1 <= i2 ? 1 : 0;
                case GE -> i1 >= i2 ? 1 : 0;
            };
        } else if (op instanceof ShiftExp.Op) {
            return switch ((ShiftExp.Op) op) {
                case SHL -> i1 << i2;
                case SHR -> i1 >> i2;
                case USHR -> i1 >>> i2;
            };
        }
        throw new AnalysisException("Unexpected op: " + op);
    }
}
