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

package pascal.taie.pta.core.heap;

import pascal.taie.language.classes.JMethod;
import pascal.taie.language.types.Type;

import java.util.Objects;
import java.util.Optional;

/**
 * Objects managed/created by Java runtime environment.
 */
public class EnvObj implements Obj {

    /**
     * Description of this object.
     */
    private final String descr;

    private final Type type;

    private final JMethod containerMethod;

    public EnvObj(String descr, Type type, JMethod containerMethod) {
        this.descr = descr;
        this.type = type;
        this.containerMethod = containerMethod;
    }

    @Override
    public String getAllocation() {
        return descr;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public Optional<JMethod> getContainerMethod() {
        return Optional.ofNullable(containerMethod);
    }

    @Override
    public Type getContainerType() {
        // TODO: set a better container type?
        return containerMethod != null
                ? containerMethod.getDeclaringClass().getType()
                : type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EnvObj envObj = (EnvObj) o;
        return descr.equals(envObj.descr)
                && Objects.equals(containerMethod, envObj.containerMethod);
    }

    @Override
    public int hashCode() {
        return Objects.hash(descr, containerMethod);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("EnvObj{");
        if (containerMethod != null) {
            sb.append(containerMethod).append("/");
        }
        sb.append(descr).append('}');
        return sb.toString();
    }
}
