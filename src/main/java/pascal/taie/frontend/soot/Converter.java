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

package pascal.taie.frontend.soot;

import pascal.taie.ir.proginfo.FieldRef;
import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JClassLoader;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.PrimitiveType;
import pascal.taie.language.type.Type;
import pascal.taie.language.type.TypeManager;
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
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static pascal.taie.language.type.VoidType.VOID;
import static pascal.taie.util.collection.MapUtils.newConcurrentMap;

/**
 * Converts Soot classes to Tai-e's representation.
 */
class Converter {

    private final JClassLoader loader;

    private final TypeManager typeManager;

    // Following four maps may be concurrently written during IR construction,
    // thus we use concurrent map to ensure their thread-safety.
    private final ConcurrentMap<SootField, JField> fieldMap
            = newConcurrentMap(4096);

    private final ConcurrentMap<SootMethod, JMethod> methodMap
            = newConcurrentMap(4096);

    private final ConcurrentMap<SootFieldRef, FieldRef> fieldRefMap
            = newConcurrentMap(4096);

    private final ConcurrentMap<SootMethodRef, MethodRef> methodRefMap
            = newConcurrentMap(4096);

    Converter(JClassLoader loader, TypeManager typeManager) {
        this.loader = loader;
        this.typeManager = typeManager;
    }

    Type convertType(soot.Type sootType) {
        if (sootType instanceof PrimType) {
            if (sootType instanceof ByteType) {
                return PrimitiveType.BYTE;
            } else if (sootType instanceof ShortType) {
                return PrimitiveType.SHORT;
            } else if (sootType instanceof IntType) {
                return PrimitiveType.INT;
            } else if (sootType instanceof LongType) {
                return PrimitiveType.LONG;
            } else if (sootType instanceof FloatType) {
                return PrimitiveType.FLOAT;
            } else if (sootType instanceof DoubleType) {
                return PrimitiveType.DOUBLE;
            } else if (sootType instanceof CharType) {
                return PrimitiveType.CHAR;
            } else if (sootType instanceof BooleanType) {
                return PrimitiveType.BOOLEAN;
            }
        } else if (sootType instanceof RefType) {
            return typeManager.getClassType(loader, sootType.toString());
        } else if (sootType instanceof ArrayType) {
            ArrayType arrayType = (ArrayType) sootType;
            return typeManager.getArrayType(
                    convertType(arrayType.baseType),
                    arrayType.numDimensions);
        } else if (sootType instanceof VoidType) {
            return VOID;
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
            List<ClassType> exceptions = m.getExceptions()
                    .stream()
                    .map(SootClass::getType)
                    .map(t -> (ClassType) convertType(t))
                    .collect(Collectors.toList());
            // TODO: convert attributes
            return new JMethod(convertClass(m.getDeclaringClass()),
                    m.getName(), Modifiers.convert(m.getModifiers()),
                    paramTypes, returnType, exceptions, sootMethod);
        });
    }

    FieldRef convertFieldRef(SootFieldRef sootFieldRef) {
        return fieldRefMap.computeIfAbsent(sootFieldRef, ref -> {
            JClass cls = convertClass(ref.declaringClass());
            Type type = convertType(ref.type());
            return FieldRef.get(cls, ref.name(), type, ref.isStatic());
        });
    }

    MethodRef convertMethodRef(SootMethodRef sootMethodRef) {
        return methodRefMap.computeIfAbsent(sootMethodRef, ref -> {
            JClass cls = convertClass(ref.getDeclaringClass());
            List<Type> paramTypes = ref.getParameterTypes()
                    .stream()
                    .map(this::convertType)
                    .collect(Collectors.toList());
            Type returnType = convertType(ref.getReturnType());
            return MethodRef.get(cls, ref.getName(), paramTypes, returnType,
                    ref.isStatic());
        });
    }

    <S, T> Collection<T> convertCollection(
            Collection<S> collection, Function<S, T> mapper) {
        if (collection.isEmpty()) {
            return Collections.emptyList();
        } else {
            return collection.stream()
                    .map(mapper)
                    .collect(Collectors.toUnmodifiableList());
        }
    }
}
