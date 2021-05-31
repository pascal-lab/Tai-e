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

package pascal.taie.ir.stmt;

import pascal.taie.ir.exp.Exp;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

/**
 * Representation of statements.
 */
public interface Stmt {

    /**
     * @return the index of this Stmt in the container IR.
     */
    int getIndex();

    void setIndex(int index);

    /**
     * @return the line number of this Stmt in the original source file.
     * If the line number is unavailable, return -1.
     */
    int getLineNumber();

    void setLineNumber(int lineNumber);

    /**
     * @return the (optional) expression defined in this Stmt.
     * In Tai-e IR, each Stmt can define at most one expression.
     */
    Optional<Exp> getDef();

    /**
     * @return a list of expressions used in this Stmt.
     */
    List<Exp> getUses();

    /**
     * @return if execution after this statement can continue at the following statement.
     */
    boolean canFallThrough();

    /**
     * Convenient API for converting this Stmt to Invoke. If this is Invoke,
     * then casts this Stmt to Invoke and returns it; otherwise, returns null.
     */
    @Nullable Invoke toInvoke();

    void accept(StmtVisitor visitor);

    <T> T accept(StmtRVisitor<T> visitor);
}
