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

package pascal.taie.frontend.soot;

import pascal.taie.ir.DefaultNewIR;
import pascal.taie.ir.NewIR;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.java.classes.JMethod;
import soot.Body;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

class MethodIRBuilder {

    private final JMethod method;

    private Set<Var> vars;

    private List<Stmt> stmts;

    MethodIRBuilder(JMethod method) {
        this.method = method;
    }

    NewIR build() {
        Body body = method.getSootMethod().retrieveActiveBody();
        Var thisVar = null;
        List<Var> params = buildParams(body);
        vars = new LinkedHashSet<>();
        stmts = new ArrayList<>();
        buildStmts(body);
        return new DefaultNewIR(method, thisVar, params, vars, stmts);
    }

    private List<Var> buildParams(Body body) {
        return Collections.emptyList();
    }

    private void buildStmts(Body body) {

    }
}
