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

package pascal.taie.type;

import pascal.taie.java.JClass;

public class ClassType implements ReferenceType {

    private final JClass jclass;

    public ClassType(JClass jclass) {
        this.jclass = jclass;
    }

    public JClass getJClass() {
        return jclass;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClassType classType = (ClassType) o;
        return jclass.equals(classType.jclass);
    }

    @Override
    public int hashCode() {
        return jclass.hashCode();
    }

    @Override
    public String toString() {
        return jclass.getName();
    }
}
