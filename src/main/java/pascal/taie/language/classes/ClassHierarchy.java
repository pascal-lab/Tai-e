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

import pascal.taie.ir.proginfo.FieldRef;
import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.language.types.Type;

import javax.annotation.Nullable;
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

    @Nullable JClass getClass(JClassLoader loader, String name);

    @Nullable JClass getClass(String name);

    /**
     * Get a JRE class by it name.
     *
     * @param name the class name
     * @return the {@link JClass} for name if found;
     * null if can't find the class. TODO: return Optional<JClass>?
     */
    @Nullable JClass getJREClass(String name);

    /**
     * Get a method declared in a JRE class by its signature.
     *
     * @param methodSig of the method
     * @return the {@link JMethod} for signature if found;
     * null if can't find the method. TODO: return Optional<JMethod>?
     * @throws pascal.taie.util.AnalysisException if signature is invalid.
     */
    @Nullable JMethod getJREMethod(String methodSig);

    /**
     * Get a field declared in a JRE class by its signature.
     *
     * @param fieldSig signature of the field
     * @return the {@link JField} for signature if found;
     * null if can't find the field. TODO: return Optional<JField>?
     * @throws pascal.taie.util.AnalysisException if signature is invalid.
     */
    JField getJREField(String fieldSig);

    JMethod resolveMethod(MethodRef methodRef);

    JField resolveField(FieldRef fieldRef);

    JMethod dispatch(Type receiverType, MethodRef methodRef);

    JMethod dispatch(JClass receiverClass, MethodRef methodRef);

    boolean isSubclass(JClass superclass, JClass subclass);
}
