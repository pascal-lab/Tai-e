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

package pascal.taie.pta.core.cs;

import pascal.taie.ir.exp.Var;
import pascal.taie.language.types.Type;
import pascal.taie.pta.core.context.Context;

public class CSVar extends AbstractPointer implements CSElement {

    private final Var var;

    private final Context context;

    CSVar(Var var, Context context) {
        this.var = var;
        this.context = context;
    }

    @Override
    public Context getContext() {
        return context;
    }

    public Var getVar() {
        return var;
    }

    @Override
    public Type getType() {
        return var.getType();
    }

    @Override
    public String toString() {
        return context + ":" + var.getMethod() + "/" + var.getName();
    }
}
