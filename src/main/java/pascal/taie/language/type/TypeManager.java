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

package pascal.taie.language.type;

import pascal.taie.language.classes.JClassLoader;

/**
 * This class provides APIs for retrieving types in the analyzed program.
 * For convenience, the special predefined types, i.e., primitive types,
 * null type, and void type can be directly retrieved from their own classes.
 */
public interface TypeManager {

    Type getType(JClassLoader loader, String typeName);

    Type getType(String typeName);

    ClassType getClassType(JClassLoader loader, String className);

    ClassType getClassType(String className);

    ArrayType getArrayType(Type baseType, int dimensions);

    ClassType getBoxedType(Type type);

    Type getUnboxedType(ClassType type);

    boolean isSubtype(Type supertype, Type subtype);
}
