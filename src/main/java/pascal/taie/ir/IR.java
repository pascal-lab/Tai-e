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

import pascal.taie.analysis.ResultHolder;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.proginfo.ExceptionEntry;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JMethod;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Intermediate representation for method body of non-abstract methods.
 * Each IR contains the variables and statements defined in a method.
 */
public interface IR extends Iterable<Stmt>, ResultHolder {

    /**
     * @return the method that defines the content of this IR.
     */
    JMethod getMethod();

    /**
     * @return the "this" variable in this IR.
     * If the method is static, then returns null.
     */
    @Nullable Var getThis();

    /**
     * @return the parameters in this IR ("this" variable is excluded).
     * The order of the parameters in the resulting list is the same as
     * the order they are declared in the method.
     */
    List<Var> getParams();

    /**
     * @return the i-th parameter in this IR. The indexes start from 0.
     */
    Var getParam(int i);

    /**
     * @return all returned variables. If the method return type is void,
     * then returns empty list.
     */
    List<Var> getReturnVars();

    /**
     * @return the variables in this IR.
     */
    List<Var> getVars();

    /**
     * @return the i-th Stmt in this IR. The indexes start from 0.
     */
    Stmt getStmt(int i);

    /**
     * @return a list of Stmts in this IR.
     */
    List<Stmt> getStmts();

    /**
     * @return a stream of Stmts in this IR.
     */
    default Stream<Stmt> stmts() {
        return getStmts().stream();
    }

    /**
     * @return iterator of Stmts in this IR.
     */
    @Override
    default Iterator<Stmt> iterator() {
        return getStmts().iterator();
    }

    /**
     * @return the exception entries in this IR.
     * @see ExceptionEntry
     */
    List<ExceptionEntry> getExceptionEntries();
}
