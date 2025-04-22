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

package pascal.taie.backend.bytecode;

import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.PrimitiveType;
import pascal.taie.language.type.Type;
import pascal.taie.language.type.VoidType;

import java.util.Optional;

/**
 * Utility class for computing Java method descriptors.
 */
public class BinaryUtils {

    /**
     * Computes the Java method descriptor for the given type.
     *
     * @param type the type to compute the descriptor for
     * @return the Java method descriptor as a string
     */
    static String computeDescriptor(Type type) {
        if (type instanceof ClassType) {
            return "L" + type.getName().replace('.', '/') + ";";
        } else if (type instanceof PrimitiveType) {
            return computePrimitive(type.getName()).orElseThrow();
        } else if (type instanceof VoidType) {
            return "V";
        } else if (type instanceof ArrayType arrayType) {
            return "[" + computeDescriptor(arrayType.elementType());
        } else {
            throw new IllegalArgumentException("Unknown type: " + type);
        }
    }

    /**
     * Computes the Java primitive type descriptor for the given primitive type name.
     *
     * @param type the primitive type name
     * @return the Java primitive type descriptor as an optional string.
     */
    private static Optional<String> computePrimitive(String type) {
        return switch (type) {
            case "int"     -> Optional.of("I");
            case "long"    -> Optional.of("J");
            case "short"   -> Optional.of("S");
            case "byte"    -> Optional.of("B");
            case "char"    -> Optional.of("C");
            case "float"   -> Optional.of("F");
            case "double"  -> Optional.of("D");
            case "boolean" -> Optional.of("Z");
            default -> Optional.empty();
        };
    }

    /**
     * Computes the Java method descriptor for the given method.
     *
     * @param method the method to compute the descriptor for
     * @return the Java method descriptor as a string
     */
    public static String computeDescriptor(JMethod method) {
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < method.getParamCount(); ++i) {
            sb.append(computeDescriptor(method.getParamType(i)));
        }
        sb.append(")");
        sb.append(computeDescriptor(method.getReturnType()));
        return sb.toString();
    }

}
