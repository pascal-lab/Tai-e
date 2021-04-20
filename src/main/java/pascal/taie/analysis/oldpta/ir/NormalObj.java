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

package pascal.taie.analysis.oldpta.ir;

import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;
import pascal.taie.util.HashUtils;

import java.util.Optional;

public class NormalObj extends AbstractObj {

    private Allocation allocation;

    private final JMethod containerMethod;

    public NormalObj(Type type, JMethod containerMethod) {
        super(type);
        this.containerMethod = containerMethod;
    }

    public void setAllocation(Allocation allocation) {
        this.allocation = allocation;
    }

    @Override
    public Kind getKind() {
        return Kind.NORMAL;
    }

    @Override
    public Allocation getAllocation() {
        return allocation;
    }

    @Override
    public Optional<JMethod> getContainerMethod() {
        return Optional.of(containerMethod);
    }

    @Override
    public Type getContainerType() {
        return containerMethod.getDeclaringClass().getType();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NormalObj obj = (NormalObj) o;
        return allocation.equals(obj.allocation)
                && type.equals(obj.type);
    }

    @Override
    public int hashCode() {
        return HashUtils.hash(allocation, type);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (containerMethod != null) {
            sb.append(containerMethod).append('/');
        }
        sb.append("new ")
                .append(type)
                .append('/')
                .append(allocation.getStartLineNumber());
        return sb.toString();
    }
}
