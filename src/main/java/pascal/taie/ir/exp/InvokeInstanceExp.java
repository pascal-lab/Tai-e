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

import pascal.taie.java.classes.MethodRef;

import java.util.List;

/**
 * Representation of instance invocation (virtual, interface,
 * and special) expression.
 */
public abstract class InvokeInstanceExp extends InvokeExp {

    protected final Var base;

    protected InvokeInstanceExp(MethodRef methodRef, Var base, List<Var> args) {
        super(methodRef, args);
        this.base = base;
    }

    public Var getBase() {
        return base;
    }

    @Override
    public String toString() {
        return String.format("%s %s.%s%s",
                getInvokeString(), base, methodRef, getArgsString());
    }
}
