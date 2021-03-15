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

package pascal.taie.oldpta.env.nativemodel;

import pascal.taie.java.classes.JMethod;
import pascal.taie.java.types.Type;
import pascal.taie.oldpta.ir.AbstractVariable;

import java.util.Objects;

/**
 * Mock variables for native modeling.
 */
class MockVariable extends AbstractVariable {

    /**
     * The name of each mock variable is unique.
     */
    private final String name;

    MockVariable(Type type, JMethod container, String name) {
        super(type, container);
        this.name = name;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public JMethod getContainerMethod() {
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
        return container.equals(that.container)
                && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(container, name);
    }

    @Override
    public String toString() {
        return container + "/" + name;
    }
}
