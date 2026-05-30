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

import pascal.taie.ir.exp.ArrayAccess;
import pascal.taie.ir.exp.BinaryExp;
import pascal.taie.ir.exp.CastExp;
import pascal.taie.ir.exp.Exp;
import pascal.taie.ir.exp.FieldAccess;
import pascal.taie.ir.exp.InstanceOfExp;
import pascal.taie.ir.exp.InvokeExp;
import pascal.taie.ir.exp.LValue;
import pascal.taie.ir.exp.Literal;
import pascal.taie.ir.exp.NewExp;
import pascal.taie.ir.exp.UnaryExp;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.AssignLiteral;
import pascal.taie.ir.stmt.Binary;
import pascal.taie.ir.stmt.Cast;
import pascal.taie.ir.stmt.Copy;
import pascal.taie.ir.stmt.InstanceOf;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.LoadArray;
import pascal.taie.ir.stmt.LoadField;
import pascal.taie.ir.stmt.New;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.ir.stmt.StoreArray;
import pascal.taie.ir.stmt.StoreField;
import pascal.taie.ir.stmt.Unary;
import pascal.taie.language.classes.JMethod;

/**
 * Utility functions for frontend.
 */
public final class IRUtils {

    private IRUtils() {
    }

    public static Stmt newAssignStmt(JMethod method, LValue left, Exp right) {
        if (left instanceof Var v) {
            if (right instanceof BinaryExp binaryExp) {
                return new Binary(v, binaryExp);
            } else if (right instanceof Literal l) {
                return new AssignLiteral(v, l);
            } else if (right instanceof CastExp cast) {
                return new Cast(v, cast);
            } else if (right instanceof UnaryExp unaryExp) {
                return new Unary(v, unaryExp);
            } else if (right instanceof Var v1) {
                return new Copy(v, v1);
            } else if (right instanceof FieldAccess fieldAccess) {
                return new LoadField(v, fieldAccess);
            } else if (right instanceof InvokeExp invokeExp) {
                return new Invoke(method, invokeExp, v);
            } else if (right instanceof NewExp newExp) {
                return new New(method, v, newExp);
            } else if (right instanceof ArrayAccess access) {
                return new LoadArray(v, access);
            } else if (right instanceof InstanceOfExp instanceOfExp) {
                return new InstanceOf(v, instanceOfExp);
            } else {
                throw new UnsupportedOperationException();
            }
        } else if (left instanceof ArrayAccess arrayAccess) {
            assert right instanceof Var;
            return new StoreArray(arrayAccess, (Var) right);
        } else if (left instanceof FieldAccess fieldAccess) {
            assert right instanceof Var;
            return new StoreField(fieldAccess, (Var) right);
        }
        throw new UnsupportedOperationException();
    }

    /**
     * Checks if expression may have side effects.
     */
    static boolean mayHaveSideEffect(Exp exp) {
        return !(exp instanceof Var || exp instanceof StackPhi || exp instanceof Literal);
    }
}
