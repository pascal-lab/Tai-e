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

import pascal.taie.language.type.Type;

import java.util.List;

/**
 * Representation of cast expression, e.g., (T) o.
 */
public class CastExp implements RValue {

    /**
     * The value to be casted.
     */
    private final Var value;

    private final Type castType;

    public CastExp(Var value, Type castType) {
        this.value = value;
        this.castType = castType;
    }

    public Var getValue() {
        return value;
    }

    public Type getCastType() {
        return castType;
    }

    @Override
    public Type getType() {
        return castType;
    }

    @Override
    public List<Exp> getUses() {
        return List.of(value);
    }

    @Override
    public <T> T accept(ExpVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return String.format("(%s) %s", castType, value);
    }
}
