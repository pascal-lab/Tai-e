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
import pascal.taie.java.classes.FieldReference;
import pascal.taie.java.classes.JClass;
import pascal.taie.java.classes.JClassLoader;
import pascal.taie.java.classes.JField;
import pascal.taie.java.classes.JMethod;
import pascal.taie.java.classes.MethodReference;
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
import soot.SootFieldRef;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.VoidType;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Convert Soot classes to Tai-e's representation.
 */
class Converter {

    private final JClassLoader loader;

    private final TypeManager typeManager;

    private final Map<SootField, JField> fieldMap = new HashMap<>();

    private final Map<SootMethod, JMethod> methodMap = new HashMap<>();

    private final Map<SootFieldRef, FieldReference> fieldRefMap
            = new HashMap<>();

    private final Map<SootMethodRef, MethodReference> methodRefMap
            = new HashMap<>();

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
        return fieldMap.computeIfAbsent(sootField, f ->
                new JField(convertClass(sootField.getDeclaringClass()),
                        sootField.getName(),
                        Modifiers.convert(sootField.getModifiers()),
                        convertType(sootField.getType())));
    }

    JMethod convertMethod(SootMethod sootMethod) {
        return methodMap.computeIfAbsent(sootMethod, m -> {
            List<Type> paramTypes = m.getParameterTypes()
                    .stream()
                    .map(this::convertType)
                    .collect(Collectors.toList());
            Type returnType = convertType(m.getReturnType());
            return new JMethod(convertClass(m.getDeclaringClass()),
                    m.getName(),
                    Modifiers.convert(m.getModifiers()),
                    paramTypes, returnType, sootMethod);
        });
    }

    FieldReference convertFieldRef(SootFieldRef sootFieldRef) {
        return fieldRefMap.computeIfAbsent(sootFieldRef, ref -> {
            JClass cls = convertClass(ref.declaringClass());
            Type type = convertType(ref.type());
            return FieldReference.get(cls, ref.name(), type);
        });
    }

    MethodReference convertMethodRef(SootMethodRef sootMethodRef) {
        return methodRefMap.computeIfAbsent(sootMethodRef, ref -> {
            JClass cls = convertClass(ref.getDeclaringClass());
            List<Type> paramTypes = ref.getParameterTypes()
                    .stream()
                    .map(this::convertType)
                    .collect(Collectors.toList());
            Type returnType = convertType(ref.getReturnType());
            return MethodReference.get(cls, ref.getName(), paramTypes, returnType);
        });
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
