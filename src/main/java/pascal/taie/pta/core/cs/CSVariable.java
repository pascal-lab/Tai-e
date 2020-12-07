/*
 * Panda - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Panda is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.taie.pta.core.cs;

import pascal.taie.pta.core.context.Context;
import pascal.taie.pta.element.Type;
import pascal.taie.pta.element.Variable;

public class CSVariable extends AbstractPointer implements CSElement {

    private final Variable var;

    private final Context context;

    CSVariable(Variable var, Context context) {
        this.var = var;
        this.context = context;
    }

    @Override
    public Context getContext() {
        return context;
    }

    public Variable getVariable() {
        return var;
    }

    @Override
    public Type getType() {
        return var.getType();
    }

    @Override
    public String toString() {
        return context + ":" + var;
    }
}
