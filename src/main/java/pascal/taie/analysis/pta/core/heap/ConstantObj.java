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
import pascal.taie.language.types.Type;

import java.util.Optional;

/**
 * Objects that represent constants.
 * @param <T> type of the constant.
 */
public class ConstantObj<T> implements Obj {

    private final Type type;

    private final T value;

    public ConstantObj(Type type, T value) {
        this.type = type;
        this.value = value;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public T getAllocation() {
        return value;
    }

    @Override
    public Optional<JMethod> getContainerMethod() {
        return Optional.empty();
    }

    @Override
    public Type getContainerType() {
        return type;
    }

    @Override
    public String toString() {
        return String.format("ConstantObj{%s: \"%s\"}", type, value);
    }
}
