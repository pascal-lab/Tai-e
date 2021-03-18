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

package pascal.taie.oldpta.ir;

import pascal.taie.callgraph.CallKind;
import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.language.classes.JMethod;
import soot.jimple.Stmt;

import java.util.List;

public class DefaultCallSite extends AbstractCallSite {

    public DefaultCallSite(CallKind kind) {
        super(kind);
    }

    /**
     * Temporarily holds this field for compatibility with JimpleCallGraph.
     * TODO: get rid of stmt.
     */
    private Stmt stmt;

    public void setStmt(Stmt stmt) {
        this.stmt = stmt;
    }

    public Stmt getSootStmt() {
        return stmt;
    }

    public void setMethodRef(MethodRef methodRef) {
        this.methodRef = methodRef;
    }

    public void setReceiver(Variable receiver) {
        this.receiver = receiver;
    }

    public void setArguments(List<Variable> args) {
        this.args = args;
    }

    public void setContainerMethod(JMethod containerMethod) {
        this.containerMethod = containerMethod;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DefaultCallSite that = (DefaultCallSite) o;
        return call.equals(that.call);
    }

    @Override
    public int hashCode() {
        return call.hashCode();
    }

    @Override
    public String toString() {
        // TODO: construct invokeRep without stmt.
        String invoke = stmt.getInvokeExpr().toString();
        String invokeRep = invoke.substring(invoke.indexOf(' ') + 1);
        return containerMethod.getDeclaringClass()
                + "(L" + call.getStartLineNumber() + "):"
                + invokeRep;
    }
}