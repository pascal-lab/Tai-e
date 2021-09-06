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

/**
 * Representation of binary expression.
 */
public interface BinaryExp extends RValue {

    /**
     * Representation of binary operators.
     */
    interface Op {
    }

    /**
     * @return the operator.
     */
    Op getOperator();

    /**
     * @return the first operand.
     */
    Var getOperand1();

    /**
     * @return the second operand.
     */
    Var getOperand2();
}
