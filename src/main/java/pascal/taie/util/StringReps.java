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

    public static String getDeclaringClass(String signature) {
        if (signature.charAt(0) != '<' &&
                signature.charAt(signature.length() - 1) != '>') {
            throw new AnalysisException(signature + " is not valid signature");
        }
        int index = signature.indexOf(":");
        if (index < 0) {
            throw new AnalysisException(signature + " is not valid signature");
        }
        return signature.substring(1, index);
    }

    public static String getSignatureOf(JMethod method) {
        return "<" +
                method.getDeclaringClass() + ": " +
                getSubsignatureOf(method) +
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

    public static String getSubsignatureOf(JMethod method) {
        return method.getReturnType() + " " +
                method.getName() +
                "(" +
                method.getParameterTypes()
                        .stream()
                        .map(Type::toString)
                        .collect(Collectors.joining(",")) +
                ")";
    }

    public static String getSubsignatureOf(MethodReference methodRef) {
        throw new UnsupportedOperationException();
    }

    public static String getSubsignatureOf(String methodSig) {
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
}
