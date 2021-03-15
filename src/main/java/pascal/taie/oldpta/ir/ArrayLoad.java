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

package pascal.taie.oldpta.ir;

/**
 * Represents an array load: to = base[*];
 */
public class ArrayLoad extends AbstractStatement {

    private final Variable to;

    private final Variable base;

    public ArrayLoad(Variable to, Variable base) {
        this.to = to;
        this.base = base;
        base.addArrayLoad(this);
    }

    public Variable getTo() {
        return to;
    }

    public Variable getBase() {
        return base;
    }

    @Override
    public void accept(StatementVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return to + " = " + base + "[*]";
    }
}
