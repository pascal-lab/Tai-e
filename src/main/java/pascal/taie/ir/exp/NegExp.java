/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2020-- Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020-- Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * Tai-e is only for educational and academic purposes,
 * and any form of commercial use is disallowed.
 * Distribution of Tai-e is disallowed without the approval.
 */

package pascal.taie.ir.exp;


import pascal.taie.language.type.PrimitiveType;

/**
 * Representation of negation expression, e.g., -o;
 */
public class NegExp implements UnaryExp {

    private final Var value;

    public NegExp(Var value) {
        this.value = value;
        assert value.getType() instanceof PrimitiveType;
    }

    public Var getValue() {
        return value;
    }

    @Override
    public Var getOperand() {
        return value;
    }

    @Override
    public PrimitiveType getType() {
        PrimitiveType type = (PrimitiveType) value.getType();
        return type.asInt() ? PrimitiveType.INT : type;
    }

    @Override
    public <T> T accept(ExpVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return "-" + value;
    }
}
