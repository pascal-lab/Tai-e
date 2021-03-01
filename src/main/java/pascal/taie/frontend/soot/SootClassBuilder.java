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

import java.util.Collection;
import java.util.Set;

class SootClassBuilder implements JClassBuilder {

    private final Converter converter;

    private final SootClass sootClass;

    SootClassBuilder(Converter converter, SootClass sootClass) {
        this.converter = converter;
        this.sootClass = sootClass;
    }

    @Override
    public void build(JClass jclass) {
        jclass.build(this);
    }

    @Override
    public Set<Modifier> getModifiers() {
        return Modifiers.convert(sootClass.getModifiers());
    }

    @Override
    public ClassType getClassType() {
        return (ClassType) converter.convertType(sootClass.getType());
    }

    @Override
    public JClass getSuperClass() {
        if (sootClass.getName().equals("java.lang.Object")) {
            return null;
        } else {
            return converter.convertClass(sootClass.getSuperclass());
        }
    }

    @Override
    public Collection<JClass> getInterfaces() {
        return converter.convertCollection(sootClass.getInterfaces(),
                converter::convertClass);
    }

    @Override
    public Collection<JField> getDeclaredFields() {
        return converter.convertCollection(sootClass.getFields(),
                converter::convertField);
    }

    @Override
    public Collection<JMethod> getDeclaredMethods() {
        return converter.convertCollection(sootClass.getMethods(),
                converter::convertMethod);
    }
}
