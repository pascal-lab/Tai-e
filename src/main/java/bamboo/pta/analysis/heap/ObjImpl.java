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

package bamboo.pta.analysis.heap;

import bamboo.pta.element.Method;
import bamboo.pta.element.Obj;
import bamboo.pta.element.Type;

/**
 * A general implementation of Obj.
 */
class ObjImpl implements Obj {

    private final Object allocation;

    private final Type type;

    private final Method containerMethod;

    ObjImpl(Object allocation, Type type, Method containerMethod) {
        this.allocation = allocation;
        this.type = type;
        this.containerMethod = containerMethod;
    }

    @Override
    public Object getAllocationSite() {
        return allocation;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public Method getContainerMethod() {
        return containerMethod;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ObjImpl obj = (ObjImpl) o;
        return allocation.equals(obj.allocation);
    }

    @Override
    public int hashCode() {
        return allocation.hashCode();
    }

    @Override
    public String toString() {
        return allocation.toString();
    }
}
