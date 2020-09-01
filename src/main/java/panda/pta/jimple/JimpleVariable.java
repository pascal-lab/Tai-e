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

package panda.pta.jimple;

import panda.pta.element.AbstractVariable;
import soot.Local;

class JimpleVariable extends AbstractVariable {

    private final Local var;

    public JimpleVariable(Local var, JimpleType type, JimpleMethod containerMethod) {
        super(type, containerMethod);
        this.var = var;
    }

    @Override
    public String getName() {
        return var.getName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JimpleVariable that = (JimpleVariable) o;
        return var.equals(that.var);
    }

    @Override
    public int hashCode() {
        return var.hashCode();
    }

    @Override
    public String toString() {
        return container + "/" + var.getName();
    }
}
