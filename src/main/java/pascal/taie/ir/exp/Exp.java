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
 * Representation of expressions in Tai-e IR.
 */
public interface Exp {

    /**
     * @return type of this expression.
     */
    Type getType();

    /**
     * @return a list of expressions which are used by (contained in) this Exp.
     */
    default List<RValue> getUses() {
        return List.of();
    }

    <T> T accept(ExpVisitor<T> visitor);
}
