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

package pascal.taie.analysis.pta.plugin.invokedynamic;

import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.ir.exp.InvokeDynamic;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;
import pascal.taie.util.HashUtils;

import java.util.Optional;

/**
 * Representation of lambda functional objects.
 */
class LambdaObj implements Obj {

    private final Type type;

    private final InvokeDynamic allocation;

    private final JMethod container;

    LambdaObj(Type type, InvokeDynamic allocation, JMethod container) {
        this.type = type;
        this.allocation = allocation;
        this.container = container;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public InvokeDynamic getAllocation() {
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
        LambdaObj lambdaObj = (LambdaObj) o;
        return type.equals(lambdaObj.type) &&
                allocation.equals(lambdaObj.allocation);
    }

    @Override
    public int hashCode() {
        return HashUtils.hash(allocation, type);
    }

    @Override
    public String toString() {
        return "LambdaObj{" +
                "type=" + type +
                ", allocation=" + allocation +
                ", container=" + container +
                '}';
    }
}
