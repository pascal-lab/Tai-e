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

package pascal.taie.ir;

public abstract class AbstractBinaryExp implements BinaryExp {

    protected final Atom value1;

    protected final Atom value2;

    protected AbstractBinaryExp(Atom value1, Atom value2) {
        this.value1 = value1;
        this.value2 = value2;
        validate();
    }

    /**
     * Validate type correctness of the two values of this expression.
     */
    protected void validate() {
    }

    @Override
    public Atom getValue1() {
        return value1;
    }

    @Override
    public Atom getValue2() {
        return value2;
    }

    @Override
    public String toString() {
        return value1 + " " + getOperator() + " " + value2;
    }
}
