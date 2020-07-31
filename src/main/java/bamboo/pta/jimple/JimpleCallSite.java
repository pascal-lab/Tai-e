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

import java.util.Collections;
import java.util.List;

class JimpleCallSite implements CallSite {

    private final Stmt stmt;

    private final CallKind kind;

    private Call call;

    private JimpleMethod method;

    private JimpleVariable receiver;

    private List<Variable> arguments = Collections.emptyList();

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

    @Override
    public CallKind getKind() {
        return kind;
    }

    @Override
    public Call getCall() {
        return call;
    }

    void setCall(Call call) {
        this.call = call;
        if (kind != CallKind.STATIC) {
            receiver.addCall(call);
        }
    }

    @Override
    public JimpleMethod getMethod() {
        return method;
    }

    void setMethod(JimpleMethod method) {
        this.method = method;
    }

    @Override
    public JimpleVariable getReceiver() {
        return receiver;
    }

    void setReceiver(JimpleVariable receiver) {
        this.receiver = receiver;
    }

    @Override
    public List<Variable> getArguments() {
        return arguments;
    }

    void setArguments(List<Variable> arguments) {
        this.arguments = arguments;
    }

    @Override
    public JimpleMethod getContainerMethod() {
        return containerMethod;
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
