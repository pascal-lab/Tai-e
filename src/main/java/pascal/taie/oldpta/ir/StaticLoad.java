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

import pascal.taie.ir.proginfo.FieldRef;

/**
 * Represents a static load: to = T.field.
 */
public class StaticLoad extends AbstractStatement {

    private final Variable to;

    private final FieldRef fieldRef;

    public StaticLoad(Variable to, FieldRef fieldRef) {
        this.to = to;
        this.fieldRef = fieldRef;
    }

    public Variable getTo() {
        return to;
    }

    public FieldRef getFieldRef() {
        return fieldRef;
    }

    @Override
    public void accept(StatementVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return to + " = " + fieldRef;
    }
}
