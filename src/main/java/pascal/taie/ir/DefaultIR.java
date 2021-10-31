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

import pascal.taie.analysis.AbstractResultHolder;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.proginfo.ExceptionEntry;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JMethod;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

/**
 * Default implementation of IR.
 * The data structures in this class are immutable.
 */
public class DefaultIR extends AbstractResultHolder implements IR {

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
        this.params = List.copyOf(params);
        this.returnVars = List.copyOf(returnVars);
        this.vars = List.copyOf(vars);
        this.stmts = List.copyOf(stmts);
        this.exceptionEntries = List.copyOf(exceptionEntries);
    }

    @Override
    public JMethod getMethod() {
        return method;
    }

    @Override
    public @Nullable Var getThis() {
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
    public Stmt getStmt(int i) {
        return stmts.get(i);
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
