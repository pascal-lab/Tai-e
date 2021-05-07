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

package pascal.taie.analysis.pta.core.heap;

import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;
import pascal.taie.util.HashUtils;

import java.util.Optional;

public abstract class MockObj implements Obj {

    private final Type type;

    private final Object allocation;

    private final JMethod container;

    public MockObj(Type type, Object allocation, JMethod container) {
        this.type = type;
        this.allocation = allocation;
        this.container = container;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public Object getAllocation() {
        return allocation;
    }

    @Override
    public Optional<JMethod> getContainerMethod() {
        return Optional.of(container);
    }

    @Override
    public Type getContainerType() {
        return container.getDeclaringClass().getType();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MockObj mockObj = (MockObj) o;
        return allocation.equals(mockObj.allocation) &&
                type.equals(mockObj.type);
    }

    @Override
    public int hashCode() {
        return HashUtils.hash(allocation, type);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "type=" + type +
                ", allocation=" + allocation +
                ", container=" + container +
                '}';
    }
}
