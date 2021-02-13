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

public abstract class MemberReference {

    private final JClass declaringClass;

    private final String name;

    public MemberReference(JClass declaringClass, String name) {
        this.declaringClass = declaringClass;
        this.name = name;
    }

    public JClass getDeclaringClass() {
        return declaringClass;
    }

    public String getName() {
        return name;
    }
}
