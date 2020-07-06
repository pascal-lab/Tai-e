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

import bamboo.pta.element.AbstractObj;
import bamboo.pta.element.Method;
import bamboo.pta.element.Type;

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
    public Object getAllocation() {
        return klass;
    }

    @Override
    public Method getContainerMethod() {
        return null;
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
        return "[Class]:" + klass;
    }
}
