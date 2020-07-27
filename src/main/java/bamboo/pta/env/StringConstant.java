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

import java.util.Optional;

/**
 * Represents string constants.
 */
class StringConstant extends AbstractObj {

    private final String value;

    StringConstant(Type type, String value) {
        super(type);
        this.value = value;
    }

    @Override
    public Kind getKind() {
        return Kind.STRING_CONSTANT;
    }

    @Override
    public Object getAllocation() {
        return value;
    }

    @Override
    public Optional<Method> getContainerMethod() {
        // String constants do not have a container method, as the same
        // string constant can appear in multiple methods.
        return Optional.empty();
    }

    @Override
    public Type getContainerType() {
        // Uses java.lang.String as the container type.
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StringConstant that = (StringConstant) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return "\"" + value + "\"";
    }
}
