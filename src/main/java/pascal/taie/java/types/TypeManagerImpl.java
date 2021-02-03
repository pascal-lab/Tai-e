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

package pascal.taie.java.types;

import pascal.taie.java.ClassHierarchy;
import pascal.taie.java.TypeManager;
import pascal.taie.java.classes.JClass;
import pascal.taie.java.classes.JClassLoader;

import java.util.HashMap;
import java.util.Map;

public class TypeManagerImpl implements TypeManager {

    private static final Map<String, PrimitiveType> primTypes;

    static {
        primTypes = new HashMap<>();
        for (PrimitiveType type : PrimitiveType.values()) {
            primTypes.put(type.getName(), type);
        }
    }

    private final ClassHierarchy classHierarchy;

    public TypeManagerImpl(ClassHierarchy classHierarchy) {
        this.classHierarchy = classHierarchy;
    }

    @Override
    public PrimitiveType getPrimitiveType(String typeName) {
        return primTypes.get(typeName);
    }

    @Override
    public ClassType getClassType(JClassLoader loader, String className) {
        return null;
    }

    @Override
    public ClassType getClassType(String className) {
        return null;
    }

    @Override
    public void addClassType(JClass jclass) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ArrayType getArrayType(Type baseType, int dimensions) {
        assert !(baseType instanceof VoidType)
                && !(baseType instanceof NullType);
        assert dimensions >= 1;
        throw new UnsupportedOperationException();
    }

    @Override
    public VoidType getVoidType() {
        return VoidType.INSTANCE;
    }

    @Override
    public NullType getNullType() {
        return NullType.INSTANCE;
    }
}
