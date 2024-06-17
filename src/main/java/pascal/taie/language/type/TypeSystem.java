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

    Type getType(JClassLoader loader, String typeName);

    Type getType(String typeName);

    ClassType getClassType(JClassLoader loader, String className);

    ClassType getClassType(String className);

    ArrayType getArrayType(Type baseType, int dimensions);

    PrimitiveType getPrimitiveType(String typeName);

    ClassType getBoxedType(PrimitiveType type);

    PrimitiveType getUnboxedType(ClassType type);

    boolean isSubtype(Type supertype, Type subtype);

    boolean isPrimitiveType(String typeName);
}
