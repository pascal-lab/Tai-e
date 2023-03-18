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

package pascal.taie.analysis.bugfinder.nullpointer;

import pascal.taie.ir.exp.ArrayLengthExp;
import pascal.taie.ir.exp.InstanceFieldAccess;
import pascal.taie.ir.exp.InvokeInstanceExp;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.LoadArray;
import pascal.taie.ir.stmt.LoadField;
import pascal.taie.ir.stmt.Monitor;
import pascal.taie.ir.stmt.StmtVisitor;
import pascal.taie.ir.stmt.StoreArray;
import pascal.taie.ir.stmt.StoreField;
import pascal.taie.ir.stmt.Throw;
import pascal.taie.ir.stmt.Unary;

class NPEVarVisitor implements StmtVisitor<Var> {

    @Override
    public Var visit(LoadField stmt) {
        return stmt.isStatic() ?
                null : ((InstanceFieldAccess) stmt.getFieldAccess()).getBase();
    }

    @Override
    public Var visit(StoreField stmt) {
        return stmt.isStatic() ?
                null : ((InstanceFieldAccess) stmt.getFieldAccess()).getBase();
    }

    @Override
    public Var visit(Unary stmt) {
        return stmt.getRValue() instanceof ArrayLengthExp ?
                ((ArrayLengthExp) stmt.getRValue()).getBase() : null;
    }

    @Override
    public Var visit(Invoke stmt) {
        return stmt.isStatic() || stmt.isDynamic() ?
                null : ((InvokeInstanceExp) stmt.getInvokeExp()).getBase();
    }

    @Override
    public Var visit(Throw stmt) {
        return stmt.getExceptionRef();
    }

    @Override
    public Var visit(Monitor stmt) {
        return StmtVisitor.super.visit(stmt);
    }

    @Override
    public Var visit(LoadArray stmt) {
        return stmt.getArrayAccess().getBase();
    }

    @Override
    public Var visit(StoreArray stmt) {
        return stmt.getArrayAccess().getBase();
    }
}
