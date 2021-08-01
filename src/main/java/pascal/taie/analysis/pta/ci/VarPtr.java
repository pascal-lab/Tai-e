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

package pascal.taie.analysis.pta.ci;

import pascal.taie.ir.exp.Var;

/**
 * Represents variable pointer in PFG.
 */
class VarPtr extends Pointer {

    private final Var var;

    VarPtr(Var var) {
        this.var = var;
    }

    Var getVar() {
        return var;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        VarPtr varPtr = (VarPtr) o;
        return var.equals(varPtr.var);
    }

    @Override
    public int hashCode() {
        return var.hashCode();
    }

    @Override
    public String toString() {
        return var.getMethod() + "/" + var.getName();
    }
}
