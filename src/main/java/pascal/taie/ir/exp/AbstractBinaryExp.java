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
import pascal.taie.language.type.ReferenceType;
import pascal.taie.language.type.Type;

import java.util.List;

/**
 * Provides common functionalities for {@link BinaryExp} implementations.
 */
abstract class AbstractBinaryExp implements BinaryExp {

    protected final Var operand1;

    protected final Var operand2;

    protected AbstractBinaryExp(Var operand1, Var operand2) {
        this.operand1 = operand1;
        this.operand2 = operand2;
        validate();
    }

    /**
     * Validates type correctness of the two values of this expression.
     */
    protected void validate() {
    }

    @Override
    public Var getOperand1() {
        return operand1;
    }

    @Override
    public Var getOperand2() {
        return operand2;
    }

    @Override
    public List<RValue> getUses() {
        return List.of(operand1, operand2);
    }

    @Override
    public String toString() {
        return operand1 + " " + getOperator() + " " + operand2;
    }

    // Convenient methods for subclasses to validate value types.

    /**
     * Obtain the computational type of given variable.
     * JVM Spec. (11 Ed., 2.11.1): most operations on values of actual types
     * boolean, byte, char, and short are correctly performed by instructions
     * operating on values of computational type int.
     *
     * @return the computational type of given variable.
     */
    private static Type getComputationalTypeOf(Var var) {
        Type type = var.getType();
        if (type instanceof PrimitiveType) {
            switch ((PrimitiveType) type) {
                case BOOLEAN:
                case BYTE:
                case CHAR:
                case SHORT:
                    return PrimitiveType.INT;
            }
        }
        return type;
    }

    /**
     * @return true if the type of given variable is computed as int, otherwise false.
     * @see #getComputationalTypeOf(Var)
     */
    protected boolean isIntLike(Var var) {
        return getComputationalTypeOf(var).equals(PrimitiveType.INT);
    }

    protected boolean isLong(Var var) {
        return var.getType().equals(PrimitiveType.LONG);
    }

    protected boolean isIntLikeOrLong(Var var) {
        return isIntLike(var) || isLong(var);
    }

    protected boolean isPrimitive(Var var) {
        return var.getType() instanceof PrimitiveType;
    }

    protected boolean isReference(Var var) {
        return var.getType() instanceof ReferenceType;
    }
}
