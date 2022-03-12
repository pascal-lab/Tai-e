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

package pascal.taie.analysis.dataflow.analysis.constprop;

import pascal.taie.ir.exp.ArithmeticExp;
import pascal.taie.ir.exp.BinaryExp;
import pascal.taie.ir.exp.BitwiseExp;
import pascal.taie.ir.exp.ConditionExp;
import pascal.taie.ir.exp.Exp;
import pascal.taie.ir.exp.IntLiteral;
import pascal.taie.ir.exp.ShiftExp;
import pascal.taie.ir.exp.Var;
import pascal.taie.language.type.PrimitiveType;
import pascal.taie.language.type.Type;
import pascal.taie.util.AnalysisException;

/**
 * Provides utilities for constant propagation. Since some functions are used
 * not only by {@link ConstantPropagation} by also other classes, we extract
 * these functions to make them easily accessible.
 */
public class CPUtils {

    private CPUtils() {
    }

    /**
     * @return true if the given variable can hold integer value, otherwise false.
     */
    public static boolean canHoldInt(Var var) {
        Type type = var.getType();
        if (type instanceof PrimitiveType) {
            switch ((PrimitiveType) type) {
                case BYTE:
                case SHORT:
                case INT:
                case CHAR:
                case BOOLEAN:
                    return true;
            }
        }
        return false;
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
            return canHoldInt(var) ? in.get(var) : Value.getNAC();
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
            } else if (v1.isNAC() || v2.isNAC()) {
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
