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

package pascal.taie.language;

import pascal.taie.language.classes.JClassLoader;
import pascal.taie.language.types.ArrayType;
import pascal.taie.language.types.ClassType;
import pascal.taie.language.types.Type;

/**
 * This class provides APIs for retrieving types in the analyzed program.
 * For convenience, the special predefined types, i.e., primitive types,
 * null type, and void type can be directly retrieved from their own classes.
 */
public interface TypeManager {

    ClassType getClassType(JClassLoader loader, String className);

    ClassType getClassType(String className);

    ArrayType getArrayType(Type baseType, int dimensions);

    boolean isSubtype(Type supertype, Type subtype);
}
