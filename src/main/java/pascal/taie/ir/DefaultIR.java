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

package pascal.taie.ir;

import pascal.taie.analysis.AbstractHolder;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.proginfo.ExceptionEntry;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JMethod;

import java.util.List;
import java.util.Set;

import static pascal.taie.util.collection.ListUtils.freeze;

/**
 * Default implementation of IR.
 */
public class DefaultIR extends AbstractHolder implements IR {

    private final JMethod method;

    private final Var thisVar;

    private final List<Var> params;

    private final List<Var> vars;

    private final List<Var> returnVars;

    private final List<Stmt> stmts;

    private final List<ExceptionEntry> exceptionEntries;

    public DefaultIR(
            JMethod method, Var thisVar,
            List<Var> params, Set<Var> returnVars, List<Var> vars,
            List<Stmt> stmts, List<ExceptionEntry> exceptionEntries) {
        this.method = method;
        this.thisVar = thisVar;
        this.params = freeze(params);
        this.returnVars = freeze(returnVars);
        this.vars = freeze(vars);
        this.stmts = freeze(stmts);
        this.exceptionEntries = freeze(exceptionEntries);
    }

    @Override
    public JMethod getMethod() {
        return method;
    }

    public Var getThis() {
        return thisVar;
    }

    @Override
    public List<Var> getParams() {
        return params;
    }

    @Override
    public Var getParam(int i) {
        return params.get(i);
    }

    @Override
    public List<Var> getReturnVars() {
        return returnVars;
    }

    @Override
    public List<Var> getVars() {
        return vars;
    }

    @Override
    public Stmt getStmt(int index) {
        return stmts.get(index);
    }

    @Override
    public List<Stmt> getStmts() {
        return stmts;
    }

    @Override
    public List<ExceptionEntry> getExceptionEntries() {
        return exceptionEntries;
    }
}
