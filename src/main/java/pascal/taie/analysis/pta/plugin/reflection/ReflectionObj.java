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

package pascal.taie.analysis.pta.plugin.reflection;

import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.language.classes.ClassMember;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;

import java.util.Optional;

/**
 * Representation of reflection meta objects.
 */
class ReflectionObj implements Obj {

    private final Type type;

    private final ClassMember member;

    ReflectionObj(Type type, ClassMember member) {
        this.type = type;
        this.member = member;
    }

    @Override
    public ClassMember getAllocation() {
        return member;
    }

    boolean isConstructor() {
        return member instanceof JMethod &&
                ((JMethod) member).isConstructor();
    }

    JMethod getConstructor() {
        assert isConstructor() : this + " is not a Constructor";
        return (JMethod) member;
    }

    boolean isMethod() {
        return member instanceof JMethod &&
                !((JMethod) member).isConstructor();
    }

    JMethod getMethod() {
        assert isMethod() : this + " is not a Method";
        return (JMethod) member;
    }

    boolean isField() {
        return member instanceof JField;
    }

    JField getField() {
        assert isField() : this + " is not a Field";
        return (JField) member;
    }

    @Override
    public Type getType() {
        return type;
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
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ReflectionObj that = (ReflectionObj) o;
        return member.equals(that.member);
    }

    @Override
    public int hashCode() {
        return member.hashCode();
    }

    @Override
    public String toString() {
        return "ReflectionObj{" +
                "type=" + type +
                ", member=" + member +
                '}';
    }
}
