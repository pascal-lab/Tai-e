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
import pascal.taie.language.type.Type;
import pascal.taie.util.Hashes;

import java.util.List;

import static pascal.taie.language.classes.ClassNames.METHOD_TYPE;
import static pascal.taie.language.classes.StringReps.toDescriptor;

/**
 * Representation of java.lang.invoke.MethodType instances.
 */
public class MethodType implements ReferenceLiteral {

    private final List<Type> paramTypes;

    private final Type returnType;

    private MethodType(List<Type> paramTypes, Type returnType) {
        this.paramTypes = List.copyOf(paramTypes);
        this.returnType = returnType;
    }

    public static MethodType get(List<Type> paramTypes, Type returnType) {
        return new MethodType(paramTypes, returnType);
    }

    public List<Type> getParamTypes() {
        return paramTypes;
    }

    public Type getReturnType() {
        return returnType;
    }

    @Override
    public ClassType getType() {
        return World.get().getTypeSystem().getClassType(METHOD_TYPE);
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
        MethodType that = (MethodType) o;
        return paramTypes.equals(that.paramTypes) &&
                returnType.equals(that.returnType);
    }

    @Override
    public int hashCode() {
        return Hashes.hash(paramTypes, returnType);
    }

    @Override
    public String toString() {
        return "MethodType: " + toDescriptor(paramTypes, returnType);
    }
}
