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
import pascal.taie.util.ArrayMap;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

public class ClassHierarchyImpl implements ClassHierarchy {

    private JClassLoader defaultLoader;

    // TODO: properly organize class loaders
    private final Map<String, JClassLoader> loaders = new ArrayMap<>();

    @Override
    public void setDefaultClassLoader(JClassLoader loader) {
        this.defaultLoader = loader;
        loaders.put("default", loader);
    }

    @Override
    public JClassLoader getDefaultClassLoader() {
        return defaultLoader;
    }

    @Override
    public Collection<JClassLoader> getClassLoaders() {
        return loaders.values();
    }

    @Override
    public Collection<JClass> getAllClasses() {
        return loaders.values().stream()
                .map(JClassLoader::getLoadedClasses)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
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
