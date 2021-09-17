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
import pascal.taie.language.type.Type;

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
     * Obtains a method class by its signature.
     *
     * @param methodSig of the method
     * @return the {@link JMethod} for signature if found;
     * null if can't find the method.
     *  TODO: return Optional<JMethod>?
     * @throws pascal.taie.util.AnalysisException if signature is invalid.
     */
    @Nullable
    JMethod getMethod(String methodSig);

    /**
     * Obtains a field by its signature.
     *
     * @param fieldSig signature of the field
     * @return the {@link JField} for signature if found;
     * null if can't find the field. TODO: return Optional<JField>?
     * @throws pascal.taie.util.AnalysisException if signature is invalid.
     */
    @Nullable
    JField getField(String fieldSig);

    /**
     * Obtains a JRE class by it name.
     *
     * @param name the class name
     * @return the {@link JClass} for name if found;
     * null if can't find the class. TODO: return Optional<JClass>?
     */
    @Nullable
    JClass getJREClass(String name);

    /**
     * Obtains a method declared in a JRE class by its signature.
     *
     * @param methodSig of the method
     * @return the {@link JMethod} for signature if found;
     * null if can't find the method. TODO: return Optional<JMethod>?
     * @throws pascal.taie.util.AnalysisException if signature is invalid.
     */
    @Nullable
    JMethod getJREMethod(String methodSig);

    /**
     * Obtains a field declared in a JRE class by its signature.
     *
     * @param fieldSig signature of the field
     * @return the {@link JField} for signature if found;
     * null if can't find the field. TODO: return Optional<JField>?
     * @throws pascal.taie.util.AnalysisException if signature is invalid.
     */
    @Nullable
    JField getJREField(String fieldSig);

    /**
     * Resolves a method reference.
     *
     * @return the concrete method pointed by the method reference,
     * or null if the concrete method cannot be found in the class hierarchy.
     */
    @Nullable
    JMethod resolveMethod(MethodRef methodRef);

    /**
     * Resolves a field reference.
     *
     * @return the concrete field pointed by the field reference,
     * or null if the concrete field cannot be found in the class hierarchy.
     */
    @Nullable
    JField resolveField(FieldRef fieldRef);

    /**
     * Dispatches a method reference on a receiver type.
     *
     * @return the target method. If the target cannot be found, returns null.
     * @throws pascal.taie.util.AnalysisException if given receiver type
     *                                            cannot be dispatched (e.g., given a primitive type).
     */
    @Nullable
    JMethod dispatch(Type receiverType, MethodRef methodRef);

    /**
     * Dispatches a method reference on a receiver class.
     *
     * @return the target method. If the target cannot be found, returns null.
     */
    @Nullable
    JMethod dispatch(JClass receiverClass, MethodRef methodRef);

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
    Collection<JClass> getDirectSubClassesOf(JClass jclass);

    boolean isSubclass(JClass superclass, JClass subclass);

    /**
     * Returns all subclasses of the given class.
     * If the given class is an interface, then return all its
     * direct/indirect subinterfaces and their all direct/indirect implementors;
     * otherwise, return all its direct/indirect subclasses.
     *
     * @param jclass      the given class.
     * @param selfInclude whether the result contains jclass itself
     */
    Collection<JClass> getAllSubclassesOf(JClass jclass, boolean selfInclude);
}
