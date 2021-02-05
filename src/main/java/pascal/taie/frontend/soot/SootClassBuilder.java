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

import pascal.taie.java.classes.JClass;
import pascal.taie.java.classes.JClassBuilder;
import pascal.taie.java.classes.JField;
import pascal.taie.java.classes.JMethod;
import pascal.taie.java.classes.Modifier;
import pascal.taie.java.types.ClassType;
import soot.SootClass;
import soot.SootField;
import soot.util.Chain;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public class SootClassBuilder implements JClassBuilder {

    private final SootClass sootClass;

    private final SootClassLoader loader;

    public SootClassBuilder(SootClass sootClass, SootClassLoader loader) {
        this.sootClass = sootClass;
        this.loader = loader;
    }

    @Override
    public Set<Modifier> getModifiers() {
        return Modifiers.convert(sootClass.getModifiers());
    }

    @Override
    public ClassType getClassType() {
        throw new UnsupportedOperationException();
    }

    @Override
    public JClass getSuperClass() {
        if (sootClass.getName().equals("java.lang.Object")) {
            return null;
        } else {
            return getJClass(sootClass.getSuperclass());
        }
    }

    @Override
    public Collection<JClass> getInterfaces() {
        Chain<SootClass> interfaces = sootClass.getInterfaces();
        if (interfaces.isEmpty()) {
            return Collections.emptyList();
        } else {
            return interfaces.stream()
                    .map(this::getJClass)
                    .collect(Collectors.toList());
        }
    }

    @Override
    public Collection<JField> getDeclaredFields() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<JMethod> getDeclaredMethods() {
        throw new UnsupportedOperationException();
    }

    private JClass getJClass(SootClass sootClass) {
        return loader.loadClass(sootClass.getName());
    }

    private JField getJField(SootField sootField) {
        throw new UnsupportedOperationException();
    }
}
