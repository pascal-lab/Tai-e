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

package bamboo.pta.jimple;

import bamboo.pta.element.AbstractObj;
import bamboo.pta.element.Method;
import bamboo.pta.element.Type;
import soot.jimple.AssignStmt;

import java.util.Objects;
import java.util.Optional;

public class JimpleObj extends AbstractObj {

    private final Object allocation;

    private final JimpleMethod containerMethod;

    JimpleObj(Object allocation, JimpleType type, JimpleMethod containerMethod) {
        super(type);
        this.allocation = allocation;
        this.containerMethod = containerMethod;
    }

    @Override
    public Kind getKind() {
        return Kind.NORMAL;
    }

    @Override
    public Object getAllocation() {
        return allocation;
    }

    @Override
    public Optional<Method> getContainerMethod() {
        return Optional.of(containerMethod);
    }

    @Override
    public Type getContainerType() {
        return containerMethod.getClassType();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JimpleObj obj = (JimpleObj) o;
        return allocation.equals(obj.allocation)
                && type.equals(obj.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(allocation, type);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (containerMethod != null) {
            sb.append(containerMethod).append('/');
        }
        if (allocation instanceof AssignStmt) {
            AssignStmt alloc = (AssignStmt) allocation;
            sb.append("new ")
                    .append(type)
                    .append('/')
                    .append(alloc.getJavaSourceStartLineNumber());
        } else {
            sb.append(allocation);
        }
        return sb.toString();
    }
}
