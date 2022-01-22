/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2020-- Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020-- Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * Tai-e is only for educational and academic purposes,
 * and any form of commercial use is disallowed.
 * Distribution of Tai-e is disallowed without the approval.
 */

package pascal.taie.language.classes;

import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.language.type.Type;
import pascal.taie.util.AnalysisException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for compute string representations of various program
 * elements, such as class name, method descriptor, method signature, etc.
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
        if (signature.charAt(0) != '<' &&
                signature.charAt(signature.length() - 1) != '>') {
            throw new AnalysisException(signature + " is not valid signature");
        }
        int index = signature.indexOf(":");
        if (index < 0) {
            throw new AnalysisException(signature + " is not valid signature");
        }
    }

    public static String getBaseTypeNameOf(String arrayTypeName) {
        return arrayTypeName.replace("[]", "");
    }

    /**
     * Converts type descriptor in bytecode to Tai-e's type descriptor.
     */
    public static String toTaieTypeDesc(String desc) {
        int i = desc.lastIndexOf('[');
        int dimensions = i + 1;
        if (dimensions > 0) { // desc is an array descriptor
            desc = desc.substring(i + 1);
        }
        String baseType;
        if (desc.charAt(0)  == 'L' &&
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
}
