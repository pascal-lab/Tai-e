/*
 * Panda - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Panda is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package panda.pta.env;

import panda.pta.core.ProgramManager;
import panda.pta.element.Type;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Manages the reflection meta objects.
 */
class ReflectionObjectPool {

    private final Type CLASS;
    private Type methodType;
    private Type fieldType;
    private Type constructorType;
    private final ConcurrentMap<Type, ClassObj> classMap
            = new ConcurrentHashMap<>();

    ReflectionObjectPool(ProgramManager pm) {
        CLASS = pm.getUniqueTypeByName("java.lang.Class");
    }

    ClassObj getClassObj(Type klass) {
        return classMap.computeIfAbsent(klass, c -> new ClassObj(CLASS, c));
    }
}
