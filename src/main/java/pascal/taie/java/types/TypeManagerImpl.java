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
import pascal.taie.java.classes.JClassLoader;
import pascal.taie.util.ArrayMap;

import java.util.HashMap;
import java.util.Map;

// TODO:
//  1. mount ArrayType to element type (like Soot)?
//  2. optimize maps (classTypes and arrayTypes)
public class TypeManagerImpl implements TypeManager {

    private static final Map<String, PrimitiveType> primTypes;

    static {
        primTypes = new HashMap<>();
        for (PrimitiveType type : PrimitiveType.values()) {
            primTypes.put(type.getName(), type);
        }
    }

    private final ClassHierarchy hierarchy;

    private final Map<JClassLoader, Map<String, ClassType>> classTypes = new ArrayMap<>();

    private final Map<Integer, Map<Type, ArrayType>> arrayTypes = new ArrayMap<>();

    public TypeManagerImpl(ClassHierarchy hierarchy) {
        this.hierarchy = hierarchy;
    }

    @Override
    public PrimitiveType getPrimitiveType(String typeName) {
        return primTypes.get(typeName);
    }

    @Override
    public PrimitiveType getByteType() {
        return PrimitiveType.BYTE;
    }

    @Override
    public PrimitiveType getShortType() {
        return PrimitiveType.SHORT;
    }

    @Override
    public PrimitiveType getIntType() {
        return PrimitiveType.INT;
    }

    @Override
    public PrimitiveType getLongType() {
        return PrimitiveType.LONG;
    }

    @Override
    public PrimitiveType getFloatType() {
        return PrimitiveType.FLOAT;
    }

    @Override
    public PrimitiveType getDoubleType() {
        return PrimitiveType.DOUBLE;
    }

    @Override
    public PrimitiveType getCharType() {
        return PrimitiveType.CHAR;
    }

    @Override
    public PrimitiveType getBooleanType() {
        return PrimitiveType.BOOLEAN;
    }

    @Override
    public ClassType getClassType(JClassLoader loader, String className) {
        return classTypes.computeIfAbsent(loader, l -> new HashMap<>())
                .computeIfAbsent(className, name -> new ClassType(loader, name));
    }

    @Override
    public ClassType getClassType(String className) {
        // TODO: add warning
        return getClassType(hierarchy.getDefaultClassLoader(), className);
    }

    @Override
    public ArrayType getArrayType(Type baseType, int dim) {
        assert !(baseType instanceof VoidType)
                && !(baseType instanceof NullType);
        assert dim >= 1;
        return arrayTypes.computeIfAbsent(dim, d -> new HashMap<>())
                .computeIfAbsent(baseType, t ->
                        new ArrayType(t, dim
                                , dim == 1 ? t : getArrayType(t, dim - 1)));
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
