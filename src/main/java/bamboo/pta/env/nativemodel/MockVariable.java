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

package bamboo.pta.env.nativemodel;

import bamboo.pta.element.AbstractVariable;
import bamboo.pta.element.Method;
import bamboo.pta.element.Type;

class MockVariable extends AbstractVariable {

    /**
     * The name of each mock variable is unique.
     */
    private final String name;

    MockVariable(Type type, Method container, String name) {
        super(type, container);
        this.name = name;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public Method getContainerMethod() {
        return container;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MockVariable that = (MockVariable) o;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return container + "/" + name;
    }
}
