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

import pascal.taie.java.classes.JClassLoader;
import pascal.taie.java.types.ArrayType;
import pascal.taie.java.types.ClassType;
import pascal.taie.java.types.NullType;
import pascal.taie.java.types.PrimitiveType;
import pascal.taie.java.types.Type;
import pascal.taie.java.types.VoidType;

public interface TypeManager {

    PrimitiveType getByteType();

    PrimitiveType getShortType();

    PrimitiveType getIntType();

    PrimitiveType getLongType();

    PrimitiveType getFloatType();

    PrimitiveType getDoubleType();

    PrimitiveType getCharType();

    PrimitiveType getBooleanType();

    ClassType getClassType(JClassLoader loader, String className);

    ClassType getClassType(String className);

    ArrayType getArrayType(Type baseType, int dimensions);

    VoidType getVoidType();

    NullType getNullType();

    boolean canAssign(Type to, Type from);
}
