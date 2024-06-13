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

import pascal.taie.language.type.BooleanType;
import pascal.taie.language.type.ReferenceType;

import java.util.Set;

/**
 * Representation of instanceof expression, e.g., o instanceof T.
 */
public class InstanceOfExp implements RValue {

    /**
     * The value to be checked.
     */
    private final Var value;

    private final ReferenceType checkedType;

    public InstanceOfExp(Var value, ReferenceType checkedType) {
        this.value = value;
        this.checkedType = checkedType;
    }

    public Var getValue() {
        return value;
    }

    public ReferenceType getCheckedType() {
        return checkedType;
    }

    @Override
    public BooleanType getType() {
        return BooleanType.BOOLEAN;
    }

    @Override
    public Set<RValue> getUses() {
        return Set.of(value);
    }

    @Override
    public <T> T accept(ExpVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return value + " instanceof " + checkedType;
    }
}
