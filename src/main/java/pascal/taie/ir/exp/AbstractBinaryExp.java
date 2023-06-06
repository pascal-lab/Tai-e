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

import pascal.taie.util.collection.ArraySet;

import java.util.Collections;
import java.util.Set;

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
    public Set<RValue> getUses() {
        Set<RValue> uses = new ArraySet<>(2);
        Collections.addAll(uses, operand1, operand2);
        return uses;
    }

    @Override
    public String toString() {
        return operand1 + " " + getOperator() + " " + operand2;
    }
}
