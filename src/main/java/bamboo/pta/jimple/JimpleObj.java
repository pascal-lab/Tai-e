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

import bamboo.pta.element.Method;
import bamboo.pta.element.Obj;
import bamboo.pta.element.Type;
import soot.jimple.AssignStmt;

import java.util.Objects;

public class JimpleObj implements Obj {

    private final Object allocation;

    private final JimpleType type;

    private final JimpleMethod containerMethod;

    JimpleObj(Object allocation, JimpleType type, JimpleMethod containerMethod) {
        this.allocation = allocation;
        this.type = type;
        this.containerMethod = containerMethod;
    }

    @Override
    public Object getAllocation() {
        return allocation;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public Method getContainerMethod() {
        return containerMethod;
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
