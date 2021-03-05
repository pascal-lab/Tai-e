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

package pascal.taie.ir.exp;

import pascal.taie.java.classes.FieldRef;
import pascal.taie.java.types.Type;

/**
 * Representation of field access expressions.
 */
public abstract class FieldAccess implements Exp {

    protected final FieldRef fieldRef;

    protected FieldAccess(FieldRef fieldRef) {
        this.fieldRef = fieldRef;
    }

    public FieldRef getFieldRef() {
        return fieldRef;
    }

    @Override
    public Type getType() {
        return fieldRef.getType();
    }
}
