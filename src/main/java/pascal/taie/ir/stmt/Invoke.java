/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2020-- Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020-- Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * Tai-e is only for educational and academic purposes,
 * and any form of commercial use is disallowed.
 * Distribution of Tai-e is disallowed without the approval.
 */

package pascal.taie.ir.stmt;

import pascal.taie.ir.exp.InvokeExp;
import pascal.taie.ir.exp.InvokeInstanceExp;
import pascal.taie.ir.exp.InvokeStatic;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.ir.proginfo.ProgramPoint;
import pascal.taie.language.classes.JMethod;

import javax.annotation.Nullable;

/**
 * Representation of invocation statement, e.g., r = o.m(..).
 */
public class Invoke extends AbstractStmt {

    private final InvokeExp invokeExp;

    private final Var result;

    public Invoke(JMethod method, InvokeExp invokeExp, Var result) {
        this.invokeExp = invokeExp;
        this.result = result;
        if (invokeExp instanceof InvokeInstanceExp) {
            Var base = ((InvokeInstanceExp) invokeExp).getBase();
            base.addInvoke(this);
        }
        invokeExp.setCallSite(new ProgramPoint(method, this));
    }

    public Invoke(JMethod method, InvokeExp invokeExp) {
        this(method, invokeExp, null);
    }

    public InvokeExp getInvokeExp() {
        return invokeExp;
    }

    public @Nullable Var getResult() {
        return result;
    }

    public MethodRef getMethodRef() {
        return invokeExp.getMethodRef();
    }

    public boolean isStatic() {
        return invokeExp instanceof InvokeStatic;
    }

    @Override
    public boolean canFallThrough() {
        return true;
    }

    @Override
    public void accept(StmtVisitor visitor) {
        visitor.visit(this);
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
