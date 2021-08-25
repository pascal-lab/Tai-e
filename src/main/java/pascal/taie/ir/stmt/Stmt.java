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

import pascal.taie.ir.exp.LValue;
import pascal.taie.ir.exp.RValue;

import java.util.List;
import java.util.Optional;

/**
 * Representation of statements in Tai-e IR.
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
     * @return the (optional) left-value expression defined in this Stmt.
     * In Tai-e IR, each Stmt can define at most one expression.
     */
    Optional<LValue> getDef();

    /**
     * @return a list of right-value expressions used in this Stmt.
     */
    List<RValue> getUses();

    /**
     * @return true if execution after this statement could continue at
     * the following statement, otherwise false.
     */
    boolean canFallThrough();

    <T> T accept(StmtVisitor<T> visitor);
}
