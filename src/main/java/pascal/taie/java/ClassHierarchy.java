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

package pascal.taie.java;

import pascal.taie.java.classes.FieldReference;
import pascal.taie.java.classes.JClass;
import pascal.taie.java.classes.JClassLoader;
import pascal.taie.java.classes.JField;
import pascal.taie.java.classes.JMethod;
import pascal.taie.java.classes.MethodReference;

import java.util.Collection;

/**
 * Manages the classes and class-related resolution of the program being analyzed.
 */
public interface ClassHierarchy {

    void setDefaultClassLoader(JClassLoader loader);

    JClassLoader getDefaultClassLoader();

    Collection<JClassLoader> getClassLoaders();

    Collection<JClass> getAllClasses();

    default JClass getClass(JClassLoader loader, String name) {
        return loader.loadClass(name);
    }

    JClass getClass(String name);

    JMethod resolveMethod(MethodReference methodRef);

    JField resolveField(FieldReference fieldRef);

    JMethod dispatch(JClass receiverClass, MethodReference methodRef);

    boolean isSubclass(JClass superclass, JClass subclass);

    boolean isAssignable(JClass fromClass, JClass toClass);
}
