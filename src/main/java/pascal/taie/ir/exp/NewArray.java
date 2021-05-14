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

import pascal.taie.language.type.ArrayType;

import java.util.List;

/**
 * Representation of new array expression, e.g., new T[..].
 */
public class NewArray extends NewExp {

    private final ArrayType type;

    private final Var length;

    public NewArray(ArrayType type, Var length) {
        this.type = type;
        this.length = length;
    }

    @Override
    public ArrayType getType() {
        return type;
    }

    public Var getLength() {
        return length;
    }

    @Override
    public List<Exp> getUses() {
        return List.of(length);
    }

    @Override
    public <T> T accept(ExpVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return String.format("newarray (%s)[%s]", type.getElementType(), length);
    }
}
