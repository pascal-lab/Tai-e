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

package pascal.taie.ir.stmt;

import pascal.taie.ir.exp.InvokeExp;
import pascal.taie.ir.exp.InvokeInstanceExp;
import pascal.taie.ir.exp.Var;

import javax.annotation.Nullable;

/**
 * Representation of invocation statement, e.g., r = o.m(..).
 */
public class Invoke extends AbstractStmt {

    private final InvokeExp invokeExp;

    private final Var result;

    public Invoke(InvokeExp invokeExp, Var result) {
        this.invokeExp = invokeExp;
        this.result = result;
        if (invokeExp instanceof InvokeInstanceExp) {
            Var base = ((InvokeInstanceExp) invokeExp).getBase();
            base.addInvoke(this);
        }
    }

    public Invoke(InvokeExp invokeExp) {
        this(invokeExp, null);
    }

    public InvokeExp getInvokeExp() {
        return invokeExp;
    }

    public @Nullable Var getResult() {
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (result != null) {
            sb.append(result).append(" = ");
        }
        sb.append(invokeExp);
        return sb.toString();
    }
}
