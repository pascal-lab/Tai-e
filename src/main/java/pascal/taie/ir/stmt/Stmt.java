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
     * @return if execution after this statement can continue at the following statement.
     */
    boolean canFallThrough();

    void accept(StmtVisitor visitor);
}
