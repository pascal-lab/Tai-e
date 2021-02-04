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

import java.util.Collection;
import java.util.Set;

public interface JClassBuilder {

    Set<Modifier> getModifiers();

    JClass getSuperClass();

    Collection<JClass> getInterfaces();

    Collection<JField> getDeclaredFields();

    Collection<JMethod> getDeclaredMethods();
}
