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

abstract class JumpStmt extends AbstractStmt {

    /**
     * Convert a target statement to its String representation.
     */
    public String toString(Stmt target) {
        return target == null ?
                "[unknown]" : Integer.toString(target.getIndex());
    }
}
