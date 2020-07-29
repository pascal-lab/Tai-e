/*
 * Bamboo - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Bamboo is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package bamboo.pta.env;

import bamboo.pta.core.ProgramManager;
import bamboo.pta.element.Type;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the reflection meta objects.
 */
class ReflectionObjectPool {

    private final Type CLASS;
    private Type METHOD;
    private Type FIELD;
    private Type CONSTRUCTOR;
    private final Map<Type, ClassObj> classMap =
            new ConcurrentHashMap<>();

    ReflectionObjectPool(ProgramManager pm) {
        CLASS = pm.getUniqueTypeByName("java.lang.Class");
    }

    ClassObj getClassObj(Type klass) {
        return classMap.computeIfAbsent(klass,
                c -> new ClassObj(CLASS, c));
    }
}
