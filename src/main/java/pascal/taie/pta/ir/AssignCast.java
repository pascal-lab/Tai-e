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

import pascal.taie.java.types.Type;

public class AssignCast extends AbstractStatement {


    private final Variable to;

    private final Variable from;

    /**
     * Type to be casted.
     */
    private final Type type;

    public AssignCast(Variable to, Type type, Variable from) {
        this.to = to;
        this.type = type;
        this.from = from;
    }

    public Variable getTo() {
        return to;
    }

    public Type getType() {
        return type;
    }

    public Variable getFrom() {
        return from;
    }

    @Override
    public void accept(StatementVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return to + " = (" + type + ") " + from;
    }
}
