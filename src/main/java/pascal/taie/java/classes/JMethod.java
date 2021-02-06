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

package pascal.taie.java.classes;

import pascal.taie.java.types.Type;
import pascal.taie.util.StringReps;

import java.util.List;
import java.util.Set;

public class JMethod extends ClassMember {

    private final List<Type> parameterTypes;

    private final Type returnType;

    public JMethod(JClass declaringClass, String name, Set<Modifier> modifiers,
                   List<Type> parameterTypes, Type returnType) {
        super(declaringClass, name, modifiers);
        this.parameterTypes = parameterTypes;
        this.returnType = returnType;
    }

    public boolean isAbstract() {
        return Modifier.hasAbstract(modifiers);
    }

    public boolean isNative() {
        return Modifier.hasNative(modifiers);
    }

    public List<Type> getParameterTypes() {
        return parameterTypes;
    }

    public Type getReturnType() {
        return returnType;
    }

    public Subsignature getSubsignature() {
        return Subsignature.get(StringReps.getSubsignatureOf(this));
    }

    @Override
    public String toString() {
        return StringReps.getSignatureOf(this);
    }
}
