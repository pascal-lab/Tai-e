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

package pascal.taie.frontend.soot;

import pascal.taie.java.TypeManager;
import pascal.taie.java.classes.JClass;
import pascal.taie.java.classes.JClassLoader;
import pascal.taie.java.classes.JField;
import pascal.taie.java.classes.JMethod;
import pascal.taie.java.types.Type;
import soot.ArrayType;
import soot.BooleanType;
import soot.ByteType;
import soot.CharType;
import soot.DoubleType;
import soot.FloatType;
import soot.IntType;
import soot.LongType;
import soot.PrimType;
import soot.RefType;
import soot.ShortType;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.VoidType;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Convert Soot classes to Tai-e's representation.
 */
class Converter {

    private final JClassLoader loader;

    private final TypeManager typeManager;

    public Converter(JClassLoader loader, TypeManager typeManager) {
        this.loader = loader;
        this.typeManager = typeManager;
    }

    Type convertType(soot.Type sootType) {
        if (sootType instanceof PrimType) {
            if (sootType instanceof ByteType) {
                return typeManager.getByteType();
            } else if (sootType instanceof ShortType) {
                return typeManager.getShortType();
            } else if (sootType instanceof IntType) {
                return typeManager.getIntType();
            } else if (sootType instanceof LongType) {
                return typeManager.getLongType();
            } else if (sootType instanceof FloatType) {
                return typeManager.getFloatType();
            } else if (sootType instanceof DoubleType) {
                return typeManager.getDoubleType();
            } else if (sootType instanceof CharType) {
                return typeManager.getCharType();
            } else if (sootType instanceof BooleanType) {
                return typeManager.getBooleanType();
            }
        } else if (sootType instanceof RefType) {
            return typeManager.getClassType(loader, sootType.toString());
        } else if (sootType instanceof ArrayType) {
            ArrayType arrayType = (ArrayType) sootType;
            return typeManager.getArrayType(
                    convertType(arrayType.baseType),
                    arrayType.numDimensions);
        } else if (sootType instanceof VoidType) {
            return typeManager.getVoidType();
        }
        throw new SootFrontendException("Cannot convert soot Type: " + sootType);
    }

    JClass convertClass(SootClass sootClass) {
        return loader.loadClass(sootClass.getName());
    }

    JField convertField(SootField sootField) {
        return new JField(convertClass(sootField.getDeclaringClass()),
                sootField.getName(),
                Modifiers.convert(sootField.getModifiers()),
                convertType(sootField.getType()));
    }

    JMethod convertMethod(SootMethod sootMethod) {
        List<Type> paramTypes = sootMethod.getParameterTypes()
                .stream()
                .map(this::convertType)
                .collect(Collectors.toList());
        Type returnType = convertType(sootMethod.getReturnType());
        return new JMethod(convertClass(sootMethod.getDeclaringClass()),
                sootMethod.getName(),
                Modifiers.convert(sootMethod.getModifiers()),
                paramTypes, returnType);
    }

    <S, T> Collection<T> convertCollection(
            Collection<S> collection, Function<S, T> mapper) {
        if (collection.isEmpty()) {
            return Collections.emptyList();
        } else {
            return collection.stream()
                    .map(mapper)
                    .collect(Collectors.toList());
        }
    }
}
