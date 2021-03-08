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
