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

package pascal.taie.language.type;

import pascal.taie.language.classes.JClassLoader;

import java.io.Serializable;

/**
 * This class provides APIs for retrieving types in the analyzed program.
 * For convenience, null type, void type and single primitive type
 * can be directly retrieved from their own classes.
 */
public interface TypeSystem extends Serializable {

    // ---------- APIs for retrieving commonly-used types ----------
    /**
     * @return the ClassType representing java.lang.Object.
     */
    ClassType objectType();

    /**
     * @return the ClassType representing java.io.Serializable.
     */
    ClassType serializableType();

    /**
     * @return the ClassType representing java.lang.Cloneable.
     */
    ClassType cloneableType();

    /**
     * @return the ClassType representing java.lang.String.
     */
    ClassType stringType();

    /**
     * @return the ClassType representing java.lang.reflect.Array.
     */
    ClassType arrayType();

    /**
     * @return the ClassType representing java.lang.Class.
     */
    ClassType classType();  // ← 避免与 getClassType(String) 混淆

    /**
     * @return the ClassType representing java.lang.Throwable.
     */
    ClassType throwableType();

    // ---------- APIs for retrieving arbitrary types ----------
    Type getType(JClassLoader loader, String typeName);

    Type getType(String typeName);

    ClassType getClassType(JClassLoader loader, String className);

    ClassType getClassType(String className);

    ArrayType getArrayType(Type baseType, int dimensions);

    PrimitiveType getPrimitiveType(String typeName);

    ClassType getBoxedType(PrimitiveType type);

    PrimitiveType getUnboxedType(ClassType type);

    // ---------- APIs for type checking ----------

    /**
     * Determines if {@code subtype} is a subtype of {@code supertype}
     * according to the Java type system rules.
     *
     * @param supertype the potential supertype
     * @param subtype the potential subtype
     * @return {@code true} if {@code subtype} is a subtype of {@code supertype},
     * {@code false} otherwise.
     */
    boolean isSubtype(Type supertype, Type subtype);

    /**
     * @return  if {@code left = right} is valid (assignable).
     */
    default boolean isAssignable(Type left, Type right) {
        if (left == right) {
            return true;
        } else if (left instanceof ReferenceType) {
            return isSubtype(left, right);
        } else {
            return canHoldInt(left) && canHoldInt(right);
        }
    }

    boolean isPrimitiveType(String typeName);

    /**
     * Checks if the given type can hold int values.
     *
     * @param type the type to be checked
     * @return {@code true} if the type is a primitive type
     * that can hold int values; {@code false} otherwise。
     */
    static boolean canHoldInt(Type type) {
        return type instanceof PrimitiveType p && p.asInt();
    }
}
