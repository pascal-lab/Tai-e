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

import bamboo.pta.element.Type;
import bamboo.pta.element.Variable;

/**
 * Represents a new statement: var = new T;
 */
public class Allocation implements Statement {

    private final Variable var;

    private final Object allocationSite;

    private final Type type;

    public Allocation(Variable var, Object allocationSite, Type type) {
        this.var = var;
        this.allocationSite = allocationSite;
        this.type = type;
    }

    public Variable getVar() {
        return var;
    }

    public Object getAllocationSite() {
        return allocationSite;
    }

    public Type getType() {
        return type;
    }

    @Override
    public Kind getKind() {
        return Kind.ALLOCATION;
    }

    @Override
    public String toString() {
        return var + " = " + allocationSite;
    }
}
