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
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.Indexer;
import pascal.taie.util.ResultHolder;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Intermediate representation for method body of non-abstract methods.
 * Each IR contains the variables and statements defined in a method.
 */
public interface IR extends Iterable<Stmt>, Indexer<Stmt>,
        ResultHolder, Serializable {

    /**
     * @return the method that defines the content of this IR.
     */
    JMethod getMethod();

    /**
     * @return the "this" variable in this IR.
     * If the method is static, then returns null.
     */
    @Nullable
    Var getThis();

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
     * @return {@code true} if {@code var} is a parameter of this IR.
     */
    boolean isParam(Var var);

    /**
     * @return {@code true} if {@code var} is "this" variable or a parameter
     * of this IR.
     */
    boolean isThisOrParam(Var var);

    /**
     * @return all returned variables. If the method return type is void,
     * then returns empty list.
     */
    List<Var> getReturnVars();

    /**
     * @return the i-th {@link Var} in this IR. The indexes start from 0.
     */
    Var getVar(int i);

    /**
     * @return the variables in this IR.
     */
    List<Var> getVars();

    /**
     * @return an indexer for the variables in this IR.
     */
    Indexer<Var> getVarIndexer();

    /**
     * @return the i-th {@link Stmt} in this IR. The indexes start from 0.
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
     * Convenient method to obtain Invokes in this IR.
     *
     * @param includeIndy whether include invokedynamic in the result.
     * @return a stream of Invokes in this IR.
     */
    default Stream<Invoke> invokes(boolean includeIndy) {
        return stmts()
                .filter(s -> s instanceof Invoke)
                .map(s -> (Invoke) s)
                .filter(i -> includeIndy || !i.isDynamic());
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
