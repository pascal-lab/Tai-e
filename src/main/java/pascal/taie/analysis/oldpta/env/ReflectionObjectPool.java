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

package pascal.taie.analysis.oldpta.env;

import pascal.taie.language.classes.StringReps;
import pascal.taie.language.type.Type;
import pascal.taie.language.type.TypeManager;

import java.util.concurrent.ConcurrentMap;

import static pascal.taie.util.collection.MapUtils.newConcurrentMap;

/**
 * Manages the reflection meta objects.
 */
class ReflectionObjectPool {

    private final Type CLASS;
    private Type methodType;
    private Type fieldType;
    private Type constructorType;
    private final ConcurrentMap<Type, ClassObj> classMap = newConcurrentMap();

    ReflectionObjectPool(TypeManager typeManager) {
        CLASS = typeManager.getClassType(StringReps.CLASS);
    }

    ClassObj getClassObj(Type klass) {
        return classMap.computeIfAbsent(klass, c -> new ClassObj(CLASS, c));
    }
}
