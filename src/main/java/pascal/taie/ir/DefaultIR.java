/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2022 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2022 Yue Li <yueli@nju.edu.cn>
 *
 * This file is part of Tai-e.
 *
 * Tai-e is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Tai-e is distributed in the hope that it will be useful,but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Tai-e. If not, see <https://www.gnu.org/licenses/>.
 */

package pascal.taie.ir;

import pascal.taie.ir.exp.Var;
import pascal.taie.ir.proginfo.ExceptionEntry;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.AbstractResultHolder;

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
    public Var getVar(int i) {
        return vars.get(i);
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
