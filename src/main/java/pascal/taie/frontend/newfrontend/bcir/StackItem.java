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
 * <p>A {@link StackItem} represents the value on JVM stack in the runtime.</p>
 * <p>
 * The design of this class is inspired by the "lazy evaluation" principle.
 * <p>When it's created, it will be a Taie {@link Exp}, and the {@link StackItem#originalExp},
 * {@link StackItem#e} both points to the exp.</p>
 * <p>Then there might be two cases,<p>
 * <ul>
 *     <li>This value is used as the right value of a {@link pascal.taie.ir.stmt.AssignStmt},
 *     then we just generate <code>v = e</code></li>
 *     <li>This value is needed to be converted tot a {@link Var},
 *     then we {@link StackItem#lift} it, now {@link StackItem#e} is set to this var,
 *     and {@link StackItem#originalExp} keeps the original value</li>
 * </ul>
 */
final class StackItem {
    /**
     * A <em>mutable</em> expression represent the value in Taie IR.
     * <ul>
     *     <li>When this value is created, <code>e = originalExp</code></li>
     *     <li>When this value is {@link StackItem#lift}, <code>e</code> will be a {@link Var}</li>
     * </ul>
     */
    private Exp e;

    private final Exp originalExp;

    /**
     * The bytecode instruction that <em>pushes</em> this value
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
        if (obj == null || obj.getClass() != this.getClass()) return false;
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
