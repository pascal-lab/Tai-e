/*
 * Panda - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Panda is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package panda.pta.statement;

import panda.pta.element.Field;
import panda.pta.element.Variable;

/**
 * Represents a static store: T.f = from.
 */
public class StaticStore implements Statement {

    private final Field field;

    private final Variable from;

    public StaticStore(Field field, Variable from) {
        this.field = field;
        this.from = from;
    }

    public Field getField() {
        return field;
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
        return Kind.STATIC_STORE;
    }

    @Override
    public String toString() {
        return field + " = " + from;
    }
}
