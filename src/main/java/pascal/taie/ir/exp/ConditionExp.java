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

import pascal.taie.language.types.PrimitiveType;

/**
 * Representation of condition expression, e.g., a == b.
 */
public class ConditionExp extends AbstractBinaryExp {

    public enum Op implements BinaryExp.Op {

        /** == */
        EQ("=="),
        /** != */
        NE("!="),
        /** < */
        LT("<"),
        /** > */
        GT(">"),
        /** <= */
        LE("<="),
        /** >= */
        GE(">="),
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

    public ConditionExp(Op op, Var value1, Var value2) {
        super(value1, value2);
        this.op = op;
    }

    @Override
    protected void validate() {
        assert (isIntLike(value1) && isIntLike(value2)) ||
                (isReference(value1) && isReference(value2));
    }

    @Override
    public Op getOperator() {
        return op;
    }

    @Override
    public PrimitiveType getType() {
        return PrimitiveType.INT;
    }
}
