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

package pascal.taie.frontend.newfrontend.bcir;

import org.objectweb.asm.tree.AbstractInsnNode;
import pascal.taie.ir.exp.Exp;
import pascal.taie.ir.exp.Var;

import java.util.Objects;

/**
 * Represents a value on the JVM stack during runtime.
 * <p>
 * This class is designed with the principle of "lazy evaluation" in mind.
 * When a {@code StackItem} is created, it initially holds a Taie {@link Exp} (expression).
 * The {@link #originalExp} and {@link #e} fields both point to this initial expression.
 * <p>
 * There are two primary scenarios for the use of a {@code StackItem}:
 * <ul>
 *     <li><strong>Assignment:</strong> If the value is used as the right-hand side of an
 *     {@link pascal.taie.ir.stmt.AssignStmt}, the expression is directly assigned to a variable.
 *     For example, <code>v = e</code>.</li>
 *     <li><strong>Lifting:</strong> If the value needs to be converted to a {@link Var},
 *     the {@link #lift} method is called. This method updates the {@link #e} field to
 *     reference the new variable, while {@link #originalExp} retains the original expression.</li>
 * </ul>
 * <p>
 * The {@link #origin} field stores the bytecode instruction that pushed this value onto the stack.
 */
final class StackItem {

    /**
     * A mutable expression representing the value in Taie IR.
     * <ul>
     *     <li>Initially, this field points to the same expression as {@link #originalExp}.</li>
     *     <li>After calling {@link #lift}, this field is updated to reference a {@link Var}.</li>
     * </ul>
     */
    private Exp e;

    /**
     * The original expression that this {@code StackItem} represents.
     */
    private final Exp originalExp;

    /**
     * The bytecode instruction that pushed this value onto the stack.
     */
    private final AbstractInsnNode origin;

    StackItem(Exp e, AbstractInsnNode origin) {
        this.e = e;
        this.originalExp = e;
        this.origin = origin;
    }

    Exp e() {
        return e;
    }

    Exp originalExp() {
        return originalExp;
    }

    Var var() {
        assert e instanceof Var;
        return (Var) e;
    }

    void lift(Var v) {
        this.e = v;
    }

    AbstractInsnNode origin() {
        return origin;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (StackItem) obj;
        return Objects.equals(this.e, that.e) &&
                Objects.equals(this.origin, that.origin);
    }

    @Override
    public int hashCode() {
        return Objects.hash(e, origin);
    }

    @Override
    public String toString() {
        return "StackItem[" +
                "e=" + e + ", " +
                "origin=" + origin + ']';
    }

}
