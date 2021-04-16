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

import pascal.taie.language.types.ClassType;

import java.util.Collection;
import java.util.Set;

/**
 * Each JClassBuilder builds one JClass.
 * TODO: make the relation between JClassBuilder and JClass explicit.
 */
public interface JClassBuilder {

    void build(JClass jclass);

    Set<Modifier> getModifiers();

    String getSimpleName();

    ClassType getClassType();

    JClass getSuperClass();

    Collection<JClass> getInterfaces();

    Collection<JField> getDeclaredFields();

    Collection<JMethod> getDeclaredMethods();
}
