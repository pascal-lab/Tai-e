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
 * Representation of invokeinterface expression, e.g., o.m(..).
 */
public class InvokeInterface extends InvokeInstanceExp {

    public InvokeInterface(MethodRef methodRef, Var base, List<Var> args) {
        super(methodRef, base, args);
    }

    @Override
    protected String getInvokeString() {
        return "invokeinterface";
    }
}
