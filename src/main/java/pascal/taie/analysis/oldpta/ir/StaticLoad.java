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

package pascal.taie.analysis.oldpta.ir;

import pascal.taie.ir.proginfo.FieldRef;

/**
 * Represents a static load: to = T.field.
 */
public class StaticLoad extends AbstractStatement {

    private final Variable to;

    private final FieldRef fieldRef;

    public StaticLoad(Variable to, FieldRef fieldRef) {
        this.to = to;
        this.fieldRef = fieldRef;
    }

    public Variable getTo() {
        return to;
    }

    public FieldRef getFieldRef() {
        return fieldRef;
    }

    @Override
    public void accept(StatementVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return to + " = " + fieldRef;
    }
}
