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

package pascal.taie.analysis.oldpta.ir;

/**
 * Represents a new statement: var = new T;
 */
public class Allocation extends AbstractStatement {

    private final Variable var;

    private final Obj object;

    public Allocation(Variable var, Obj object) {
        this.var = var;
        this.object = object;
        if (object instanceof NormalObj) {
            ((NormalObj) object).setAllocation(this);
        }
    }

    public Variable getVar() {
        return var;
    }

    public Obj getObject() {
        return object;
    }

    @Override
    public void accept(StatementVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return var + " = " + object;
    }
}
