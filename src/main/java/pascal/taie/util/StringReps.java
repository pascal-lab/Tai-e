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

import pascal.taie.java.classes.JField;
import pascal.taie.java.classes.JMethod;
import pascal.taie.java.classes.MethodReference;
import pascal.taie.java.types.Type;

import java.util.List;

/**
 * Utility class for compute string representations of various program
 * information, such as class name, method descriptor, method signature, etc.
 */
public class StringReps {

    // Suppresses default constructor, ensuring non-instantiability.
    private StringReps() {
    }

    public static String getSignatureOf(JMethod method) {
        throw new UnsupportedOperationException();
    }

    public static String getSignatureOf(JField field) {
        return "<" + field.getDeclaringClass() + ": " +
                field.getType() + " " +
                field.getName() + ">";
    }

    public static String getSubsignatureOf(JMethod method) {
        throw new UnsupportedOperationException();
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
