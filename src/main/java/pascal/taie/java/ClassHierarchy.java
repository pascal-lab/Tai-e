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
import pascal.taie.java.classes.Subsignature;
import pascal.taie.util.StringReps;

import java.util.Collection;

/**
 * Manages the classes and class-related resolution of the program being analyzed.
 */
public interface ClassHierarchy {

    void setDefaultClassLoader(JClassLoader loader);

    JClassLoader getDefaultClassLoader();

    void setBootstrapClassLoader(JClassLoader loader);

    JClassLoader getBootstrapClassLoader();

    Collection<JClassLoader> getClassLoaders();

    Collection<JClass> getAllClasses();

    default JClass getClass(JClassLoader loader, String name) {
        return loader.loadClass(name);
    }

    default JClass getClass(String name) {
        // TODO: add warning
        return getClass(getDefaultClassLoader(), name);
    }

    /**
     * Get a JRE class by it name.
     *
     * @param name the class name
     * @return the {@link JClass} for name if found; null if can't find the class.
     */
    default JClass getJREClass(String name) {
        return getClass(getBootstrapClassLoader(), name);
    }

    /**
     * Get a method declared in a JRE class by its signature.
     *
     * @param signature of the method
     * @return the {@link JMethod} for signature if found;
     * null if can't find the class.
     * @throws pascal.taie.util.AnalysisException if signature is invalid.
     */
    default JMethod getJREMethod(String signature) {
        String className = StringReps.getDeclaringClass(signature);
        Subsignature subsig = Subsignature.get(
                StringReps.getSubsignatureOf(signature));
        return getJREClass(className).getDeclaredMethod(subsig);
    }

    JMethod resolveMethod(MethodReference methodRef);

    JField resolveField(FieldReference fieldRef);

    JMethod dispatch(JClass receiverClass, MethodReference methodRef);

    boolean isSubclass(JClass superclass, JClass subclass);

    boolean canAssign(JClass fromClass, JClass toClass);
}
