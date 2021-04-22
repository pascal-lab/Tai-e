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

import pascal.taie.analysis.AnalysisResultHolder;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.proginfo.ExceptionEntry;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JMethod;

import java.util.List;

/**
 * Intermediate representation for method body of non-abstract methods.
 */
public interface IR extends AnalysisResultHolder {

    JMethod getMethod();

    Var getThis();

    List<Var> getParams();

    Var getParam(int i);

    List<Var> getReturnVars();

    List<Var> getVars();

    Stmt getStmt(int index);

    List<Stmt> getStmts();

    List<ExceptionEntry> getExceptionEntries();
}
