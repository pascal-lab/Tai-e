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

import pascal.taie.language.classes.JMethod;

/**
 * Utility class for compute string representations of bytecode descriptors
 */
public final class BytecodeDescriptors {

    // Suppresses default constructor, ensuring non-instantiability.
    private BytecodeDescriptors() {
    }

    /**
     * Converts type descriptor in bytecode to Tai-e's type descriptor.
     * For example:
     * <ul>
     *     <li>{@code [I} to {@code int[]}.</li>
     *     <li>{@code [[I} to {@code int[][]}.</li>
     *     <li>{@code Ljava/lang/Object;} to {@code java.lang.Object}.</li>
     *     <li>{@code [Ljava/lang/Object;} to {@code java.lang.Object[]}.</li>
     * </ul>
     */
    public static String toTaieTypeDesc(String desc) {
        int i = desc.lastIndexOf('[');
        int dimensions = i + 1;
        if (dimensions > 0) { // desc is an array descriptor
            desc = desc.substring(i + 1);
        }
        String baseType;
        if (desc.charAt(0) == 'L' &&
                desc.charAt(desc.length() - 1) == ';') {
            baseType = desc.substring(1, desc.length() - 1)
                    .replace('/', '.');
        } else {
            baseType = switch (desc.charAt(0)) {
                case 'Z' -> "boolean";
                case 'B' -> "byte";
                case 'C' -> "char";
                case 'S' -> "short";
                case 'I' -> "int";
                case 'F' -> "float";
                case 'J' -> "long";
                case 'D' -> "double";
                default -> throw new IllegalArgumentException(
                        "Invalid bytecode type descriptor: " + desc);
            };
        }
        if (dimensions == 0) {
            return baseType;
        } else {
            return baseType + "[]".repeat(dimensions);
        }
    }

    /**
     * Computes the bytecode descriptor for the given type.
     *
     * @param type the type to compute the descriptor for
     * @return the bytecode descriptor as a string
     */
    public static String toBytecodeDescriptor(Type type) {
        if (type instanceof ClassType) {
            return "L" + type.getName().replace('.', '/') + ";";
        } else if (type instanceof PrimitiveType) {
            return switch (type.getName()) {
                case "int" -> "I";
                case "long" -> "J";
                case "short" -> "S";
                case "byte" -> "B";
                case "char" -> "C";
                case "float" -> "F";
                case "double" -> "D";
                case "boolean" -> "Z";
                default -> throw new IllegalArgumentException(
                        "Unknown primitive type: " + type);
            };
        } else if (type instanceof VoidType) {
            return "V";
        } else if (type instanceof ArrayType arrayType) {
            return "[" + toBytecodeDescriptor(arrayType.elementType());
        } else {
            throw new IllegalArgumentException("Unknown type: " + type);
        }
    }

    /**
     * Computes the bytecode descriptor for the given method.
     *
     * @param method the method to compute the descriptor for
     * @return the bytecode descriptor as a string
     */
    public static String toBytecodeDescriptor(JMethod method) {
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < method.getParamCount(); ++i) {
            sb.append(toBytecodeDescriptor(method.getParamType(i)));
        }
        sb.append(")");
        sb.append(toBytecodeDescriptor(method.getReturnType()));
        return sb.toString();
    }
}
