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

/**
 * Representation of throw exception statement, e.g., throw e.
 */
public class Throw extends AbstractStmt {

    /**
     * Reference of the exception object to be thrown.
     */
    private final Var ref;

    public Throw(Var ref) {
        this.ref = ref;
    }

    public Var getRef() {
        return ref;
    }

    @Override
    public String toString() {
        return "throw " + ref;
    }
}
