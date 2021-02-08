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

package pascal.taie.java.classes;

import pascal.taie.java.ClassHierarchy;

import java.util.Collection;

public class ClassHierarchyImpl implements ClassHierarchy {

    private JClassLoader defaultLoader;

    @Override
    public void setDefaultClassLoader(JClassLoader loader) {
        this.defaultLoader = loader;
    }

    @Override
    public JClassLoader getDefaultClassLoader() {
        return defaultLoader;
    }

    @Override
    public Collection<JClassLoader> getClassLoaders() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<JClass> getAllClasses() {
        return null;
    }

    @Override
    public JClass getClass(String name) {
        // TODO: add warning
        return defaultLoader.loadClass(name);
    }

    @Override
    public JMethod resolveMethod(MethodReference methodRef) {
        throw new UnsupportedOperationException();
    }

    @Override
    public JField resolveField(FieldReference fieldRef) {
        throw new UnsupportedOperationException();
    }

    @Override
    public JMethod dispatch(JClass receiverClass, MethodReference methodRef) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSubclass(JClass superclass, JClass subclass) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isAssignable(JClass fromClass, JClass toClass) {
        throw new UnsupportedOperationException();
    }
}
