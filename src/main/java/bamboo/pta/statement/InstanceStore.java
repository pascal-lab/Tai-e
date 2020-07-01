/*
 * Bamboo - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Bamboo is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package bamboo.pta.statement;

import bamboo.pta.element.Field;
import bamboo.pta.element.Variable;

/**
 * Represents an instance store: base.field = from.
 */
public class InstanceStore implements Statement {

    private final Variable base;

    private final Field field;

    private final Variable from;

    public InstanceStore(Variable base, Field field, Variable from) {
        this.base = base;
        this.field = field;
        this.from = from;
        base.addInstanceStore(this);
    }

    public Variable getBase() {
        return base;
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
        return Kind.INSTANCE_STORE;
    }

    @Override
    public String toString() {
        return base + "." + field + " = " + from;
    }
}
