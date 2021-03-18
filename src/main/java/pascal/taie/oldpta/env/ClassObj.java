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

package pascal.taie.oldpta.env;

import pascal.taie.language.classes.JMethod;
import pascal.taie.language.types.Type;
import pascal.taie.oldpta.ir.AbstractObj;

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
    public Optional<JMethod> getContainerMethod() {
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
