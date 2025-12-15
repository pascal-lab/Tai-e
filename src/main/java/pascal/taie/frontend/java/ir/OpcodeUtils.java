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

package pascal.taie.frontend.java.ir;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnNode;
import pascal.taie.ir.exp.ArithmeticExp;
import pascal.taie.ir.exp.BitwiseExp;
import pascal.taie.ir.exp.ComparisonExp;
import pascal.taie.ir.exp.ConditionExp;
import pascal.taie.ir.exp.DoubleLiteral;
import pascal.taie.ir.exp.FloatLiteral;
import pascal.taie.ir.exp.IntLiteral;
import pascal.taie.ir.exp.Literal;
import pascal.taie.ir.exp.LongLiteral;
import pascal.taie.ir.exp.NullLiteral;
import pascal.taie.ir.exp.ShiftExp;
import pascal.taie.language.type.Type;

import static pascal.taie.language.type.ByteType.BYTE;
import static pascal.taie.language.type.CharType.CHAR;
import static pascal.taie.language.type.DoubleType.DOUBLE;
import static pascal.taie.language.type.FloatType.FLOAT;
import static pascal.taie.language.type.IntType.INT;
import static pascal.taie.language.type.LongType.LONG;
import static pascal.taie.language.type.ShortType.SHORT;

/**
 * Utility methods for JVM opcode classification and conversion to Tai-e IR operations.
 * <p>
 * This class is package-private as it is only used within the IR building process.
 */
final class OpcodeUtils {

    private OpcodeUtils() {
    }

    // ========== Opcode Range Check ==========

    static boolean isInRange(int opcode, int min, int max) {
        return opcode >= min && opcode <= max;
    }

    // ========== Instruction Classification ==========

    static boolean isConstInsn(int opcode) {
        return opcode == Opcodes.ACONST_NULL ||
                isInRange(opcode, Opcodes.ICONST_M1, Opcodes.ICONST_5) ||
                isInRange(opcode, Opcodes.FCONST_0, Opcodes.FCONST_2) ||
                isInRange(opcode, Opcodes.LCONST_0, Opcodes.LCONST_1) ||
                isInRange(opcode, Opcodes.DCONST_0, Opcodes.DCONST_1);
    }

    static boolean isAddInsn(int opcode) {
        return isInRange(opcode, Opcodes.IADD, Opcodes.DADD);
    }

    static boolean isSubInsn(int opcode) {
        return isInRange(opcode, Opcodes.ISUB, Opcodes.DSUB);
    }

    static boolean isMulInsn(int opcode) {
        return isInRange(opcode, Opcodes.IMUL, Opcodes.DMUL);
    }

    static boolean isDivInsn(int opcode) {
        return isInRange(opcode, Opcodes.IDIV, Opcodes.DDIV);
    }

    static boolean isRemInsn(int opcode) {
        return isInRange(opcode, Opcodes.IREM, Opcodes.DREM);
    }

    static boolean isNegInsn(int opcode) {
        return isInRange(opcode, Opcodes.INEG, Opcodes.DNEG);
    }

    static boolean isPrimCastInsn(int opcode) {
        return isInRange(opcode, Opcodes.I2L, Opcodes.I2S);
    }

    static boolean isBinaryInsn(int opcode) {
        return (isInRange(opcode, Opcodes.IADD, Opcodes.LXOR) && !isNegInsn(opcode)) ||
                isComparisonInsn(opcode);
    }

    static boolean isArithmeticInsn(int opcode) {
        return isInRange(opcode, Opcodes.IADD, Opcodes.DREM);
    }

    static boolean isBitwiseInsn(int opcode) {
        return isInRange(opcode, Opcodes.IAND, Opcodes.LXOR);
    }

    static boolean isShiftInsn(int opcode) {
        return isInRange(opcode, Opcodes.ISHL, Opcodes.LUSHR);
    }

    static boolean isComparisonInsn(int opcode) {
        return isInRange(opcode, Opcodes.LCMP, Opcodes.DCMPG);
    }

    static boolean isReturnInsn(int opcode) {
        return isInRange(opcode, Opcodes.IRETURN, Opcodes.RETURN);
    }

    static boolean isStackInsn(int opcode) {
        return isInRange(opcode, Opcodes.POP, Opcodes.SWAP);
    }

    static boolean isArrayLoadInsn(int opcode) {
        return isInRange(opcode, Opcodes.IALOAD, Opcodes.SALOAD);
    }

    static boolean isArrayStoreInsn(int opcode) {
        return isInRange(opcode, Opcodes.IASTORE, Opcodes.SASTORE);
    }

    // ========== Constant Value Extraction ==========

    static Literal toConstValue(InsnNode node) {
        int opcode = node.getOpcode();
        if (opcode == Opcodes.ACONST_NULL) {
            return NullLiteral.get();
        } else if (isInRange(opcode, Opcodes.ICONST_M1, Opcodes.ICONST_5)) {
            return IntLiteral.get(opcode - Opcodes.ICONST_M1 - 1);
        } else if (isInRange(opcode, Opcodes.FCONST_0, Opcodes.FCONST_2)) {
            return FloatLiteral.get(opcode - Opcodes.FCONST_0 + 0.0f);
        } else if (isInRange(opcode, Opcodes.DCONST_0, Opcodes.DCONST_1)) {
            return DoubleLiteral.get(opcode - Opcodes.DCONST_0 + 0.0);
        } else if (isInRange(opcode, Opcodes.LCONST_0, Opcodes.LCONST_1)) {
            return LongLiteral.get(opcode - Opcodes.LCONST_0);
        } else {
            throw new IllegalArgumentException("Not a constant instruction: " + opcode);
        }
    }

    // ========== Opcode Unification ==========

    static int unifyIfOp(int opcode) {
        if (isInRange(opcode, Opcodes.IFEQ, Opcodes.IFLE)) {
            return opcode - Opcodes.IFEQ + Opcodes.IF_ICMPEQ;
        } else if (isInRange(opcode, Opcodes.FCMPL, Opcodes.FCMPG)) {
            return opcode - Opcodes.FCMPL + Opcodes.DCMPL;
        } else {
            return opcode;
        }
    }

    // ========== Opcode to Tai-e IR Operator Conversion ==========

    static ConditionExp.Op toCondOp(int opcode) {
        opcode = unifyIfOp(opcode);
        return switch (opcode) {
            case Opcodes.IF_ICMPEQ, Opcodes.IF_ACMPEQ, Opcodes.IFNULL -> ConditionExp.Op.EQ;
            case Opcodes.IF_ICMPNE, Opcodes.IF_ACMPNE, Opcodes.IFNONNULL -> ConditionExp.Op.NE;
            case Opcodes.IF_ICMPLT -> ConditionExp.Op.LT;
            case Opcodes.IF_ICMPGE -> ConditionExp.Op.GE;
            case Opcodes.IF_ICMPGT -> ConditionExp.Op.GT;
            case Opcodes.IF_ICMPLE -> ConditionExp.Op.LE;
            default -> throw new IllegalArgumentException("Unknown condition opcode: " + opcode);
        };
    }

    static ArithmeticExp.Op toArithmeticOp(int opcode) {
        if (isAddInsn(opcode)) {
            return ArithmeticExp.Op.ADD;
        } else if (isSubInsn(opcode)) {
            return ArithmeticExp.Op.SUB;
        } else if (isMulInsn(opcode)) {
            return ArithmeticExp.Op.MUL;
        } else if (isDivInsn(opcode)) {
            return ArithmeticExp.Op.DIV;
        } else if (isRemInsn(opcode)) {
            return ArithmeticExp.Op.REM;
        } else {
            throw new IllegalArgumentException("Unknown arithmetic opcode: " + opcode);
        }
    }

    static BitwiseExp.Op toBitwiseOp(int opcode) {
        return switch (opcode) {
            case Opcodes.IAND, Opcodes.LAND -> BitwiseExp.Op.AND;
            case Opcodes.IOR, Opcodes.LOR -> BitwiseExp.Op.OR;
            case Opcodes.IXOR, Opcodes.LXOR -> BitwiseExp.Op.XOR;
            default -> throw new IllegalArgumentException("Unknown bitwise opcode: " + opcode);
        };
    }

    static ComparisonExp.Op toCmpOp(int opcode) {
        return switch (opcode) {
            case Opcodes.LCMP -> ComparisonExp.Op.CMP;
            case Opcodes.DCMPG, Opcodes.FCMPG -> ComparisonExp.Op.CMPG;
            case Opcodes.DCMPL, Opcodes.FCMPL -> ComparisonExp.Op.CMPL;
            default -> throw new IllegalArgumentException("Unknown comparison opcode: " + opcode);
        };
    }

    static ShiftExp.Op toShiftOp(int opcode) {
        return switch (opcode) {
            case Opcodes.ISHL, Opcodes.LSHL -> ShiftExp.Op.SHL;
            case Opcodes.ISHR, Opcodes.LSHR -> ShiftExp.Op.SHR;
            case Opcodes.IUSHR, Opcodes.LUSHR -> ShiftExp.Op.USHR;
            default -> throw new IllegalArgumentException("Unknown shift opcode: " + opcode);
        };
    }

    static Type toCastType(int opcode) {
        return switch (opcode) {
            case Opcodes.L2I, Opcodes.F2I, Opcodes.D2I -> INT;
            case Opcodes.I2L, Opcodes.F2L, Opcodes.D2L -> LONG;
            case Opcodes.I2F, Opcodes.L2F, Opcodes.D2F -> FLOAT;
            case Opcodes.I2D, Opcodes.L2D, Opcodes.F2D -> DOUBLE;
            case Opcodes.I2B -> BYTE;
            case Opcodes.I2S -> SHORT;
            case Opcodes.I2C -> CHAR;
            default -> throw new IllegalArgumentException("Unknown cast opcode: " + opcode);
        };
    }
}
