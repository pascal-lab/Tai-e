/*
 * Bamboo - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Bamboo is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package bamboo.pta.jimple;

import bamboo.callgraph.CallKind;
import bamboo.pta.element.CallSite;
import bamboo.pta.element.Variable;
import bamboo.pta.statement.Call;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;

import java.util.List;

class JimpleCallSite implements CallSite {

    private Stmt stmt;

    private CallKind kind;

    private Call call;

    private JimpleMethod method;

    private JimpleVariable receiver;

    private List<Variable> arguments;

    private JimpleMethod containerMethod;

    public JimpleCallSite(Stmt stmt, CallKind kind) {
        this.stmt = stmt;
        this.kind = kind;
    }

    Stmt getSootStmt() {
        return stmt;
    }

    InvokeExpr getSootInvokeExpr() {
        return stmt.getInvokeExpr();
    }

    void setCall(Call call) {
        this.call = call;
        if (!isStatic()) {
            receiver.addCall(call);
        }
    }

    void setMethod(JimpleMethod method) {
        this.method = method;
    }

    void setReceiver(JimpleVariable receiver) {
        this.receiver = receiver;
    }

    void setArguments(List<Variable> arguments) {
        this.arguments = arguments;
    }

    void setContainerMethod(JimpleMethod containerMethod) {
        this.containerMethod = containerMethod;
    }

    @Override
    public boolean isInterface() {
        return kind == CallKind.INTERFACE;
    }

    @Override
    public boolean isVirtual() {
        return kind == CallKind.VIRTUAL;
    }

    @Override
    public boolean isSpecial() {
        return kind == CallKind.SPECIAL;
    }

    @Override
    public boolean isStatic() {
        return kind == CallKind.STATIC;
    }

    @Override
    public Call getCall() {
        return call;
    }

    @Override
    public JimpleMethod getMethod() {
        return method;
    }

    @Override
    public JimpleVariable getReceiver() {
        return receiver;
    }

    @Override
    public List<Variable> getArguments() {
        return arguments;
    }

    @Override
    public JimpleMethod getContainerMethod() {
        return containerMethod;
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
        return  "L" +  stmt.getJavaSourceStartLineNumber()
                + "@" + containerMethod.getClassType()
                + ":" + stmt;
    }
}
