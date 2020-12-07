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

package pascal.taie.pta.env;

import pascal.taie.pta.element.AbstractObj;
import pascal.taie.pta.element.Method;
import pascal.taie.pta.element.Type;

import java.util.Optional;

/**
 * Represents class objects.
 */
public class ClassObj extends AbstractObj {

    private final Type klass;

    ClassObj(Type type, Type klass) {
        super(type);
        this.klass = klass;
    }

    @Override
    public Kind getKind() {
        return Kind.CLASS;
    }

    @Override
    public Type getAllocation() {
        return klass;
    }

    @Override
    public Optional<Method> getContainerMethod() {
        return Optional.empty();
    }

    @Override
    public Type getContainerType() {
        // Uses java.lang.Class as the container type.
        return type;
    }

    @Override
    public int hashCode() {
        return klass.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClassObj classObj = (ClassObj) o;
        return klass.equals(classObj.klass);
    }

    @Override
    public String toString() {
        return "[Class]" + klass;
    }
}
