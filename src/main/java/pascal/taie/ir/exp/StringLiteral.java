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

import pascal.taie.World;
import pascal.taie.language.type.ClassType;

import static pascal.taie.language.classes.ClassNames.STRING;

public class StringLiteral implements ReferenceLiteral {

    private final String value;

    private StringLiteral(String value) {
        this.value = value;
    }

    public static StringLiteral get(String value) {
        // TODO: canonicalize?
        return new StringLiteral(value);
    }

    @Override
    public ClassType getType() {
        // TODO: cache String type in a static field? Doing so
        //  requires to reset the field when resetting World.
        return World.get().getTypeSystem().getClassType(STRING);
    }

    public String getString() {
        return value;
    }

    @Override
    public <T> T accept(ExpVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        StringLiteral that = (StringLiteral) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return "\"" + value + "\"";
    }
}
