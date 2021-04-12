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

import pascal.taie.ir.exp.ReferenceLiteral;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.types.Type;

import java.util.Optional;

/**
 * Objects that represent constants.
 */
public class ConstantObj implements Obj {

    private final ReferenceLiteral value;

    public ConstantObj(ReferenceLiteral value) {
        this.value = value;
    }

    @Override
    public Type getType() {
        return value.getType();
    }

    @Override
    public ReferenceLiteral getAllocation() {
        return value;
    }

    @Override
    public Optional<JMethod> getContainerMethod() {
        return Optional.empty();
    }

    @Override
    public Type getContainerType() {
        return getType();
    }

    @Override
    public String toString() {
        return String.format("ConstantObj{%s: %s}", getType(), value);
    }
}
