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

package pascal.taie.java.classes;

import pascal.taie.java.types.ClassType;

import java.util.Collection;
import java.util.Set;

/**
 * Each JClassBuilder builds one JClass.
 * TODO: make the relation between JClassBuilder and JClass explicit.
 */
public interface JClassBuilder {

    void build(JClass jclass);

    Set<Modifier> getModifiers();

    ClassType getClassType();

    JClass getSuperClass();

    Collection<JClass> getInterfaces();

    Collection<JField> getDeclaredFields();

    Collection<JMethod> getDeclaredMethods();
}
