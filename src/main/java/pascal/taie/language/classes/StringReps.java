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

package pascal.taie.language.classes;

import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.language.type.Type;
import pascal.taie.util.AnalysisException;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for compute string representations of various program
 * elements, such as class name, method descriptor, method signature, etc.
 * <ul>
 *  <li>Method
 *      <ul>
 *      <li>Signature: &lt;CLASS_NAME: RETURN_TYPE METHOD_NAME(PARAM_LIST)&gt;</li>
 *      <li>Subsignature: RETURN_TYPE METHOD_NAME(PARAM_LIST)</li>
 *      </ul>
 *  </li>
 *  <li>Field
 *      <ul>
 *      <li>Signature: &lt;CLASS_NAME: FIELD_TYPE FIELD_NAME&gt;</li>
 *      </ul>
 *  </li>
 * </ul>
 */
public final class StringReps {

    // Suppresses default constructor, ensuring non-instantiability.
    private StringReps() {
    }

    public static String getClassNameOf(String signature) {
        validateSignature(signature);
        int index = signature.indexOf(":");
        return signature.substring(1, index);
    }

    public static String getSignatureOf(JMethod method) {
        return getMethodSignature(method.getDeclaringClass(), method.getName(),
                method.getParamTypes(), method.getReturnType());
    }

    public static String getMethodSignature(
            JClass declaringClass, String methodName,
            List<Type> parameterTypes, Type returnType) {
        return "<" +
                declaringClass + ": " +
                toSubsignature(methodName, parameterTypes, returnType) +
                ">";
    }

    public static String getSignatureOf(JField field) {
        return getFieldSignature(field.getDeclaringClass(),
                field.getName(), field.getType());
    }

    public static String getFieldSignature(
            JClass declaringClass, String fieldName, Type fieldType) {
        return "<" + declaringClass + ": " + fieldType + " " + fieldName + ">";
    }

    public static String getFieldNameOf(String fieldSig) {
        validateSignature(fieldSig);
        int index = fieldSig.lastIndexOf(' ');
        return fieldSig.substring(index + 1, fieldSig.length() - 1);
    }

    public static String getFieldTypeOf(String fieldSig) {
        validateSignature(fieldSig);
        int begin = fieldSig.indexOf(' ') + 1;
        int end = fieldSig.lastIndexOf(' ');
        return fieldSig.substring(begin, end);
    }

    public static String getSubsignatureOf(JMethod method) {
        return toSubsignature(method.getName(),
                method.getParamTypes(), method.getReturnType());
    }

    public static String getSubsignatureOf(MethodRef methodRef) {
        throw new UnsupportedOperationException();
    }

    public static String getSubsignatureOf(String methodSig) {
        validateSignature(methodSig);
        int index = methodSig.indexOf(":");
        return methodSig.substring(index + 2, methodSig.length() - 1);
    }

    public static String getDescriptorOf(JMethod method) {
        throw new UnsupportedOperationException();
    }

    public static String getDescriptorOf(MethodRef methodRef) {
        throw new UnsupportedOperationException();
    }

    public static String toDescriptor(List<Type> parameterTypes, Type returnType) {
        return returnType + " " +
                "(" +
                parameterTypes.stream()
                        .map(Type::toString)
                        .collect(Collectors.joining(",")) +
                ")";
    }

    public static String toSubsignature(String name, List<Type> parameterTypes, Type returnType) {
        return returnType + " " +
                name +
                "(" +
                parameterTypes.stream()
                        .map(Type::toString)
                        .collect(Collectors.joining(",")) +
                ")";
    }

    private static void validateSignature(String signature) {
        if (signature.isBlank() ||
                signature.charAt(0) != '<' &&
                signature.charAt(signature.length() - 1) != '>') {
            throw new AnalysisException(
                    "\"" + signature + "\" is not a valid signature");
        }
        int index = signature.indexOf(":");
        if (index < 0) {
            throw new AnalysisException(
                    "\"" + signature + "\" is not a valid signature");
        }
    }

    /**
     * @return {@code true} if {@code typeName} represents array type.
     */
    public static boolean isArrayType(String typeName) {
        return typeName.endsWith("[]");
    }

    /**
     * Given an array type name, returns the type name of base type of the array.
     */
    public static String getBaseTypeNameOf(String arrayTypeName) {
        return arrayTypeName.replace("[]", "");
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
     * @return {@code true} if the given string is a valid Java identifier.
     */
    public static boolean isJavaIdentifier(@Nullable String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        if (!Character.isJavaIdentifierStart(name.charAt(0))) {
            return false;
        }
        for (int i = 1; i < name.length(); i++) {
            if (!Character.isJavaIdentifierPart(name.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return {@code true} if the given string is a valid fully-qualified name
     * of a Java class.
     */
    public static boolean isJavaClassName(@Nullable String className) {
        if (className == null || className.isEmpty()) {
            return false;
        }
        boolean inIdentifierStart = true;
        for (int i = 0; i < className.length(); i++) {
            char c = className.charAt(i);
            if (inIdentifierStart) {
                if (!Character.isJavaIdentifierStart(c)) {
                    return false;
                }
                inIdentifierStart = false;
            } else {
                if (c == '.') {
                    inIdentifierStart = true;
                } else if (!Character.isJavaIdentifierPart(c)) {
                    return false;
                }
            }
        }
        return !inIdentifierStart;
    }
}
