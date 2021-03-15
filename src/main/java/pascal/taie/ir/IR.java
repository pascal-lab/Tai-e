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

package pascal.taie.ir;

import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.java.classes.JMethod;

import java.util.List;

public interface IR {

    JMethod getMethod();

    Var getThis();

    List<Var> getParams();

    Var getParam(int i);

    List<Var> getReturnVars();

    List<Var> getVars();

    List<Stmt> getStmts();

    List<ExceptionEntry> getExceptionEntries();
}
