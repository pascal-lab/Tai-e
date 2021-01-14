/*
 * Tai-e: A Program Analysis Framework for Java
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

package pascal.taie.java;

import java.util.Set;

public enum Modifier {

    PUBLIC,
    PRIVATE,
    PROTECTED,
    STATIC,
    FINAL,
    SYNCHRONIZED,
    VOLATILE,
    TRANSIENT,
    NATIVE,
    INTERFACE,
    ABSTRACT,
    STRICTFP,

    BRIDGE,
    VARARGS,
    SYNTHETIC,
    ANNOTATION,
    ENUM,
    MANDATED;

    @Override
    public String toString() {
        return name().toLowerCase();
    }

    public static boolean isPublic(Set<Modifier> modifiers) {
        return modifiers.contains(PUBLIC);
    }

    public static boolean isProtected(Set<Modifier> modifiers) {
        return modifiers.contains(PROTECTED);
    }

    public static boolean isPrivate(Set<Modifier> modifiers) {
        return modifiers.contains(PRIVATE);
    }

    public static boolean isStatic(Set<Modifier> modifiers) {
        return modifiers.contains(STATIC);
    }

    public static boolean isFinal(Set<Modifier> modifiers) {
        return modifiers.contains(FINAL);
    }

    public static boolean isSynchronized(Set<Modifier> modifiers) {
        return modifiers.contains(SYNCHRONIZED);
    }

    public static boolean isVolatile(Set<Modifier> modifiers) {
        return modifiers.contains(VOLATILE);
    }

    public static boolean isTransient(Set<Modifier> modifiers) {
        return modifiers.contains(TRANSIENT);
    }

    public static boolean isNative(Set<Modifier> modifiers) {
        return modifiers.contains(NATIVE);
    }

    public static boolean isInterface(Set<Modifier> modifiers) {
        return modifiers.contains(INTERFACE);
    }

    public static boolean isAbstract(Set<Modifier> modifiers) {
        return modifiers.contains(ABSTRACT);
    }

    public static boolean isStrictFP(Set<Modifier> modifiers) {
        return modifiers.contains(STRICTFP);
    }

    public static boolean isSynthetic(Set<Modifier> modifiers) {
        return modifiers.contains(SYNTHETIC);
    }

    public static boolean isAnnotation(Set<Modifier> modifiers) {
        return modifiers.contains(ANNOTATION);
    }

    public static boolean isEnum(Set<Modifier> modifiers) {
        return modifiers.contains(ENUM);
    }
}
