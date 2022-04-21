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

import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.Type;

import java.util.List;

/**
 * Representation of array access expression, e.g., a[i].
 */
public class ArrayAccess implements LValue, RValue {

    private final Var base;

    private final Var index;

    public ArrayAccess(Var base, Var index) {
        this.base = base;
        this.index = index;
        assert base.getType() instanceof ArrayType;
    }

    public Var getBase() {
        return base;
    }

    public Var getIndex() {
        return index;
    }

    @Override
    public Type getType() {
        if (base.getType() instanceof ArrayType) {
            return ((ArrayType) base.getType()).elementType();
        } else {
            throw new RuntimeException("Invalid base type: " + base.getType());
        }
    }

    @Override
    public List<RValue> getUses() {
        return List.of(base, index);
    }

    @Override
    public <T> T accept(ExpVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return String.format("%s[%s]", base, index);
    }
}
