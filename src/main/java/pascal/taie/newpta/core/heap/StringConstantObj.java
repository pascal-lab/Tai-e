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

package pascal.taie.newpta.core.heap;

import pascal.taie.java.World;
import pascal.taie.java.classes.JMethod;
import pascal.taie.java.classes.StringReps;
import pascal.taie.java.types.ClassType;
import pascal.taie.java.types.Type;

import java.util.Optional;

/**
 * Representation of string constants.
 */
public class StringConstantObj implements Obj {

    /**
     * Type java.lang.String.
     */
    private static ClassType stringType;

    private final String value;

    public StringConstantObj(String value) {
        this.value = value;
    }

    @Override
    public String getAllocation() {
        return value;
    }

    @Override
    public Type getType() {
        if (stringType == null) {
            stringType = World.getTypeManager()
                    .getClassType(StringReps.STRING);
        }
        return stringType;
    }

    @Override
    public Optional<JMethod> getContainerMethod() {
        // String constants do not have a container method, as the same
        // string constant can appear in multiple methods.
        return Optional.empty();
    }

    @Override
    public Type getContainerType() {
        // Uses java.lang.String as the container type.
        return getType();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        StringConstantObj that = (StringConstantObj) o;
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
