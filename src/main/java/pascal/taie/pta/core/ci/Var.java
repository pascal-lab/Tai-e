/*
 * Tai'e - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Tai'e is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.taie.pta.core.ci;

import pascal.taie.pta.element.Variable;

/**
 * Represents variable nodes in PFG.
 */
class Var extends Pointer {

    private final Variable var;

    Var(Variable var) {
        this.var = var;
    }

    Variable getVariable() {
        return var;
    }

    @Override
    public String toString() {
        return var.toString();
    }
}
