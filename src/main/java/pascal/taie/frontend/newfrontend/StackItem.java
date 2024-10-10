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

package pascal.taie.frontend.newfrontend;

import org.objectweb.asm.tree.AbstractInsnNode;
import pascal.taie.ir.exp.Exp;
import pascal.taie.ir.exp.Var;

import java.util.Objects;

final class StackItem {
    private Exp e;
    private final Exp originalExp;
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
        if (obj == this) { return true; }
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
