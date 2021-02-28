/*
 * Tai-e - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Tai-e is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.taie.util;

import pascal.taie.java.classes.FieldReference;
import pascal.taie.java.classes.JClass;
import pascal.taie.java.classes.JField;
import pascal.taie.java.classes.JMethod;
import pascal.taie.java.classes.MethodReference;
import pascal.taie.java.types.Type;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for compute string representations of various program
 * elements, such as class name, method descriptor, method signature, etc.
 */
public class StringReps {

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

    public static String getSignatureOf(MethodReference methodRef) {
        return getMethodSignature(methodRef.getDeclaringClass(),
                methodRef.getName(),
                methodRef.getParameterTypes(), methodRef.getReturnType());
    }

    private static String getMethodSignature(
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

    public static String getSignatureOf(FieldReference fieldRef) {
        return getFieldSignature(fieldRef.getDeclaringClass(),
                fieldRef.getName(), fieldRef.getType());
    }

    private static String getFieldSignature(
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

    public static String getSubsignatureOf(MethodReference methodRef) {
        throw new UnsupportedOperationException();
    }

    public static String getSubsignatureOf(String methodSig) {
        validateSignature(methodSig);
        throw new UnsupportedOperationException();
    }

    public static String getDescriptorOf(JMethod method) {
        throw new UnsupportedOperationException();
    }

    public static String getDescriptorOf(MethodReference methodRef) {
        throw new UnsupportedOperationException();
    }

    public static String toDescriptor(List<Type> parameterTypes, Type returnType) {
        throw new UnsupportedOperationException();
    }

    public static String toSubsignature(String name, List<Type> parameterTypes, Type returnType) {
        return returnType + " " +
                name +
                "(" +
                parameterTypes
                        .stream()
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
}
