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

public class MethodReference extends MemberReference {

    private final List<Type> parameterTypes;

    private final Type returnType;

    private final Subsignature subsignature;

    /**
     * Cache the resolved method for this reference to avoid redundant
     * method resolution.
     */
    private JMethod method;

    public MethodReference(JClass declaringClass, String name,
                           List<Type> parameterTypes, Type returnType) {
        super(declaringClass, name);
        this.parameterTypes = parameterTypes;
        this.returnType = returnType;
        this.subsignature = Subsignature.get(name, parameterTypes, returnType);
    }

    public List<Type> getParameterTypes() {
        return parameterTypes;
    }

    public Type getReturnType() {
        return returnType;
    }

    public Subsignature getSubsignature() {
        return subsignature;
    }

    public JMethod getMethod() {
        return method;
    }

    public void setMethod(JMethod method) {
        this.method = method;
    }

    @Override
    public String toString() {
        return StringReps.getSignatureOf(this);
    }
}
