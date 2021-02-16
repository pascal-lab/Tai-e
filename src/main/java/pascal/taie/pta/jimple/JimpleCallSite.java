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

package pascal.taie.pta.jimple;

import pascal.taie.callgraph.CallKind;
import pascal.taie.pta.ir.AbstractCallSite;
import pascal.taie.pta.ir.Variable;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;

import java.util.List;

class JimpleCallSite extends AbstractCallSite {

    private final Stmt stmt;

    public JimpleCallSite(Stmt stmt, CallKind kind) {
        super(kind);
        this.stmt = stmt;
    }

    Stmt getSootStmt() {
        return stmt;
    }

    InvokeExpr getSootInvokeExpr() {
        return stmt.getInvokeExpr();
    }

    void setMethod(JimpleMethod method) {
        this.method = method;
    }

    void setReceiver(JimpleVariable receiver) {
        this.receiver = receiver;
    }

    void setArguments(List<Variable> args) {
        this.args = args;
    }

    void setContainerMethod(JimpleMethod containerMethod) {
        this.containerMethod = containerMethod;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JimpleCallSite that = (JimpleCallSite) o;
        return stmt.equals(that.stmt);
    }

    @Override
    public int hashCode() {
        return stmt.hashCode();
    }

    @Override
    public String toString() {
        String invoke = stmt.getInvokeExpr().toString();
        String invokeRep = invoke.substring(invoke.indexOf(' ') + 1);
        return containerMethod.getClassType()
                + "(L" + stmt.getJavaSourceStartLineNumber() + "):"
                + invokeRep;
    }
}
