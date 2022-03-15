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
}
