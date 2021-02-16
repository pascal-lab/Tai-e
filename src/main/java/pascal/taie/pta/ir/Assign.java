/*
 * Tai-e - A Program Analysis Framework for Java
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

package pascal.taie.pta.ir;

/**
 * Represents a local assignment: to = from;
 */
public class Assign implements Statement {

    private final Variable to;

    private final Variable from;

    public Assign(Variable to, Variable from) {
        this.to = to;
        this.from = from;
    }

    public Variable getTo() {
        return to;
    }

    public Variable getFrom() {
        return from;
    }

    @Override
    public void accept(StatementVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public Kind getKind() {
        return Kind.ASSIGN;
    }

    @Override
    public String toString() {
        return to + " = " + from;
    }
}
