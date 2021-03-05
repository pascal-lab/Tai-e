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

import pascal.taie.ir.exp.Var;

abstract class AbstractMonitorStmt extends AbstractStmt {

    /**
     * Reference of the object to be locked/unlocked.
     */
    protected final Var objectRef;

    protected AbstractMonitorStmt(Var objectRef) {
        this.objectRef = objectRef;
    }

    public Var getObjectRef() {
        return objectRef;
    }

    protected abstract String getInsnString();

    @Override
    public String toString() {
        return getInsnString() + " " + objectRef;
    }
}
