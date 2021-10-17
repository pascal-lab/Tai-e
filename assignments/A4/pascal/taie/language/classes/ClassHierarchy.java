/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2020-- Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020-- Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * Tai-e is only for educational and academic purposes,
 * and any form of commercial use is disallowed.
 * Distribution of Tai-e is disallowed without the approval.
 */

package pascal.taie.language.classes;

import pascal.taie.ir.proginfo.MethodRef;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.stream.Stream;

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
     * Adds a JClass into class hierarchy.
     * This API should be invoked everytime {@link JClassLoader}
     * loads a new JClass.
     */
    void addClass(JClass jclass);

    Stream<JClass> allClasses();

    Stream<JClass> applicationClasses();

    @Nullable
    JClass getClass(JClassLoader loader, String name);

    @Nullable
    JClass getClass(String name);

    /**
     * @return the direct subinterfaces of given interface.
     */
    Collection<JClass> getDirectSubinterfacesOf(JClass jclass);

    /**
     * @return the direct implementors of given interface.
     */
    Collection<JClass> getDirectImplementorsOf(JClass jclass);

    /**
     * @return the direct subclasses of given class.
     */
    Collection<JClass> getDirectSubclassesOf(JClass jclass);

    /**
     * Obtains a JRE class by it name.
     *
     * @param name the class name
     * @return the {@link JClass} for name if found;
     * null if can't find the class.
     */
    @Nullable
    JClass getJREClass(String name);

    @Nullable JMethod resolveMethod(MethodRef methodRef);

    /**
     * Obtains a method declared in a JRE class by its signature.
     *
     * @param methodSig of the method
     * @return the {@link JMethod} for signature if found;
     * null if can't find the method.
     * @throws pascal.taie.util.AnalysisException if signature is invalid.
     */
    @Nullable
    JMethod getJREMethod(String methodSig);
}
