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

package pascal.taie.newpta.core.heap;

import pascal.taie.java.World;
import pascal.taie.java.classes.JMethod;
import pascal.taie.java.classes.StringReps;
import pascal.taie.java.types.ClassType;
import pascal.taie.java.types.Type;

import java.util.Optional;

/**
 * Representation of java.lang.Class objects.
 */
public class ClassObj implements Obj {

    /**
     * Type java.lang.Class.
     */
    private static ClassType classType;

    private final Type klass;

    public ClassObj(Type klass) {
        this.klass = klass;
    }

    @Override
    public Type getAllocation() {
        return klass;
    }

    @Override
    public Type getType() {
        if (classType == null) {
            classType = World.getTypeManager()
                    .getClassType(StringReps.CLASS);
        }
        return classType;
    }

    @Override
    public Optional<JMethod> getContainerMethod() {
        return Optional.empty();
    }

    @Override
    public Type getContainerType() {
        // Uses java.lang.Class as the container type.
        return getType();
    }

    @Override
    public int hashCode() {
        return klass.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o){
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ClassObj classObj = (ClassObj) o;
        return klass.equals(classObj.klass);
    }

    @Override
    public String toString() {
        return "[Class]" + klass;
    }
}
