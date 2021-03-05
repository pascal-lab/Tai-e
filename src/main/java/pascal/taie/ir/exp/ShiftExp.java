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

package pascal.taie.ir.exp;

import pascal.taie.java.types.PrimitiveType;

/**
 * Representation of shift expression, e.g., a >> b.
 */
public class ShiftExp extends AbstractBinaryExp {

    public enum Op implements BinaryExp.Op {

        /** << */
        SHL("<<"),
        /** >> */
        SHR(">>"),
        /** <<< */
        USHL("<<<"),
        /** >>> */
        USHR(">>>"),
        ;

        private final String name;

        Op(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private final Op op;

    public ShiftExp(Op op, Var value1, Var value2) {
        super(value1, value2);
        this.op = op;
    }

    @Override
    protected void validate() {
        assert value1.getType().equals(PrimitiveType.INT) ||
                value1.getType().equals(PrimitiveType.LONG);
        assert value2.getType().equals(PrimitiveType.INT);
    }

    @Override
    public Op getOperator() {
        return op;
    }

    @Override
    public PrimitiveType getType() {
        return (PrimitiveType) value1.getType();
    }
}
