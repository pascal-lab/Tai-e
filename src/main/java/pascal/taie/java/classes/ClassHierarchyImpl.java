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
import pascal.taie.util.ArraySet;
import pascal.taie.util.StringReps;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public class ClassHierarchyImpl implements ClassHierarchy {

    private JClassLoader defaultLoader;

    private JClassLoader bootstrapLoader;

    // TODO: properly manage class loaders
    private final Set<JClassLoader> loaders = new ArraySet<>();

    @Override
    public void setDefaultClassLoader(JClassLoader loader) {
        this.defaultLoader = loader;
        loaders.add(loader);
    }

    @Override
    public JClassLoader getDefaultClassLoader() {
        return defaultLoader;
    }

    @Override
    public void setBootstrapClassLoader(JClassLoader loader) {
        this.bootstrapLoader = loader;
        loaders.add(loader);
    }

    @Override
    public JClassLoader getBootstrapClassLoader() {
        return bootstrapLoader;
    }

    @Override
    public Collection<JClassLoader> getClassLoaders() {
        return loaders;
    }

    @Override
    public Collection<JClass> getAllClasses() {
        return loaders.stream()
                .map(JClassLoader::getLoadedClasses)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    @Override
    public JClass getClass(JClassLoader loader, String name) {
        return loader.loadClass(name);
    }

    @Override
    public JClass getClass(String name) {
        // TODO: add warning
        return getClass(getDefaultClassLoader(), name);
    }

    @Override
    public JClass getJREClass(String name) {
        return getClass(getBootstrapClassLoader(), name);
    }

    @Override
    public JMethod getJREMethod(String signature) {
        String className = StringReps.getDeclaringClass(signature);
        Subsignature subsig = Subsignature.get(
                StringReps.getSubsignatureOf(signature));
        return getJREClass(className).getDeclaredMethod(subsig);
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
    public boolean canAssign(JClass toClass, JClass fromClass) {
        throw new UnsupportedOperationException();
    }
}
