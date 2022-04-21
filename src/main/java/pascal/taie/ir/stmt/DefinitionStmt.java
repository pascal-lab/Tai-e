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

package pascal.taie.ir.stmt;

import pascal.taie.ir.exp.LValue;
import pascal.taie.ir.exp.RValue;

import javax.annotation.Nullable;

/**
 * Representation of all definition statements, i.e., exp1 = exp2.
 *
 * @param <L> type of left-hand side expression
 * @param <R> type of right-hand side expression
 */
public abstract class DefinitionStmt<L extends LValue, R extends RValue>
        extends AbstractStmt {

    /**
     * @return the left-hand side expression. If this Stmt is an {@link Invoke}
     * which does not have a left-hand side expression, e.g., o.m(...), then
     * this method returns null; otherwise, it must return a non-null value.
     */
    public abstract @Nullable L getLValue();

    /**
     * @return the right-hand side expression.
     */
    public abstract R getRValue();
}
