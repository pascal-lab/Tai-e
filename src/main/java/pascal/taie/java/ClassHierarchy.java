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
import pascal.taie.java.types.Type;

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

    /**
     * Add a JClass into class hierarchy.
     * This API should be invoked everytime {@link JClassLoader}
     * loads a new JClass.
     */
    void addClass(JClass jclass);

    Collection<JClass> getAllClasses();

    JClass getClass(JClassLoader loader, String name);

    JClass getClass(String name);

    /**
     * Get a JRE class by it name.
     *
     * @param name the class name
     * @return the {@link JClass} for name if found;
     * null if can't find the class. TODO: return Optional<JClass>?
     */
    JClass getJREClass(String name);

    /**
     * Get a method declared in a JRE class by its signature.
     *
     * @param methodSig of the method
     * @return the {@link JMethod} for signature if found;
     * null if can't find the method. TODO: return Optional<JMethod>?
     * @throws pascal.taie.util.AnalysisException if signature is invalid.
     */
    JMethod getJREMethod(String methodSig);

    /**
     * Get a field declared in a JRE class by its signature.
     *
     * @param fieldSig signature of the field
     * @return the {@link JField} for signature if found;
     * null if can't find the field. TODO: return Optional<JField>?
     * @throws pascal.taie.util.AnalysisException if signature is invalid.
     */
    JField getJREField(String fieldSig);

    JMethod resolveMethod(MethodReference methodRef);

    JField resolveField(FieldReference fieldRef);

    JMethod dispatch(Type receiverType, MethodReference methodRef);

    JMethod dispatch(JClass receiverClass, MethodReference methodRef);

    boolean canAssign(JClass toClass, JClass fromClass);
}
