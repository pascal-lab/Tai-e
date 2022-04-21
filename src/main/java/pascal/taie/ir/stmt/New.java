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

import pascal.taie.ir.exp.NewExp;
import pascal.taie.ir.exp.Var;
import pascal.taie.language.classes.JMethod;

/**
 * Representation of following kinds of new statements:
 * <ul>
 *     <li>new instance: o = new T
 *     <li>new array: o = new T[..]
 *     <li>new multi-array: o = new T[..][..]
 * </ul>
 */
public class New extends AssignStmt<Var, NewExp> {

    /**
     * The method containing this new statement.
     */
    private final JMethod container;

    public New(JMethod method, Var lvalue, NewExp rvalue) {
        super(lvalue, rvalue);
        this.container = method;
    }

    public JMethod getContainer() {
        return container;
    }

    @Override
    public <T> T accept(StmtVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
