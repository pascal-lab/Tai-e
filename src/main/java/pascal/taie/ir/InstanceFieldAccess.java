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

package pascal.taie.ir;

import pascal.taie.java.classes.FieldRef;

/**
 * Representation of instance field access expression, e.g., o.f.
 */
public class InstanceFieldAccess extends FieldAccess {

    private final Var base;

    public InstanceFieldAccess(FieldRef fieldRef, Var base) {
        super(fieldRef);
        this.base = base;
    }

    public Var getBase() {
        return base;
    }

    @Override
    public String toString() {
        return base + "." + fieldRef;
    }
}
