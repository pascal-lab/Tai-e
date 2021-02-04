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
import soot.SootClass;

import java.util.Collection;
import java.util.Set;

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
    public JClass getSuperClass() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<JClass> getInterfaces() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<JField> getDeclaredFields() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<JMethod> getDeclaredMethods() {
        throw new UnsupportedOperationException();
    }
}
