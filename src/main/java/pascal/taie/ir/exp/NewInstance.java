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

import pascal.taie.language.type.ClassType;

/**
 * Representation of new instance expression, e.g., new T.
 */
public class NewInstance implements NewExp {

    private final ClassType type;

    public NewInstance(ClassType type) {
        this.type = type;
    }

    @Override
    public ClassType getType() {
        return type;
    }

    @Override
    public <T> T accept(ExpVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return "new " + type;
    }
}
