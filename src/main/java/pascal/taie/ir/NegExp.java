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


import pascal.taie.java.types.PrimitiveType;

/**
 * Representation of negation expression, e.g., -o;
 */
public class NegExp implements UnaryExp {

    private final Atom value;

    public NegExp(Atom value) {
        this.value = value;
    }

    public Atom getValue() {
        return value;
    }

    @Override
    public PrimitiveType getType() {
        switch ((PrimitiveType) value.getType()) {
            case INT:
            case BYTE:
            case SHORT:
            case BOOLEAN:
            case CHAR:
                return PrimitiveType.INT;
            case LONG:
                return PrimitiveType.LONG;
            case FLOAT:
                return PrimitiveType.FLOAT;
            case DOUBLE:
                return PrimitiveType.DOUBLE;
        }
        throw new RuntimeException(
                "Invalid value type of NegExp: " + value.getType());
    }

    @Override
    public String toString() {
        return "-" + value;
    }
}
