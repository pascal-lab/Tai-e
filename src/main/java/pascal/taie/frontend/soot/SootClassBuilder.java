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

package pascal.taie.frontend.soot;

import pascal.taie.java.TypeManager;
import pascal.taie.java.World;
import pascal.taie.java.classes.JClass;
import pascal.taie.java.classes.JClassBuilder;
import pascal.taie.java.classes.JField;
import pascal.taie.java.classes.JMethod;
import pascal.taie.java.classes.Modifier;
import pascal.taie.java.types.ClassType;
import pascal.taie.java.types.Type;
import soot.ArrayType;
import soot.PrimType;
import soot.RefType;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SootClassBuilder implements JClassBuilder {

    private final SootClassLoader loader;

    private final SootClass sootClass;

    private JClass jclass;

    private final TypeManager typeManager;

    public SootClassBuilder(SootClassLoader loader, SootClass sootClass) {
        this.loader = loader;
        this.sootClass = sootClass;
        this.typeManager = World.get().getTypeManager();
    }

    @Override
    public JClass build() {
        jclass = new JClass(loader, sootClass.getName());
        jclass.build(this);
        return jclass;
    }

    @Override
    public Set<Modifier> getModifiers() {
        return Modifiers.convert(sootClass.getModifiers());
    }

    @Override
    public ClassType getClassType() {
        return typeManager.getClassType(loader, sootClass.getName());
    }

    @Override
    public JClass getSuperClass() {
        if (sootClass.getName().equals("java.lang.Object")) {
            return null;
        } else {
            return convertClass(sootClass.getSuperclass());
        }
    }

    @Override
    public Collection<JClass> getInterfaces() {
        return convertCollection(sootClass.getInterfaces(),
                this::convertClass);
    }

    @Override
    public Collection<JField> getDeclaredFields() {
        return convertCollection(sootClass.getFields(), this::convertField);
    }

    @Override
    public Collection<JMethod> getDeclaredMethods() {
        return convertCollection(sootClass.getMethods(), this::convertMethod);
    }

    private <S, T> Collection<T> convertCollection(
            Collection<S> collection, Function<S, T> mapper) {
        if (collection.isEmpty()) {
            return Collections.emptyList();
        } else {
            return collection.stream()
                    .map(mapper)
                    .collect(Collectors.toList());
        }
    }

    private Type convertType(soot.Type sootType) {
        if (sootType instanceof PrimType) {
            return typeManager.getPrimitiveType(sootType.toString());
        } else if (sootType instanceof RefType) {
            return typeManager.getClassType(loader, sootType.toString());
        } else if (sootType instanceof ArrayType) {
            ArrayType arrayType = (ArrayType) sootType;
            return typeManager.getArrayType(
                    convertType(arrayType.baseType),
                    arrayType.numDimensions);
        } else {
            throw new SootFrontendException(
                    "Cannot convert soot Type: " + sootType);
        }
    }

    private JClass convertClass(SootClass sootClass) {
        return loader.loadClass(sootClass.getName());
    }

    private JField convertField(SootField sootField) {
        return new JField(jclass, sootField.getName(),
                Modifiers.convert(sootField.getModifiers()),
                convertType(sootField.getType()));
    }

    private JMethod convertMethod(SootMethod sootMethod) {
        throw new UnsupportedOperationException();
    }
}
