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

/**
 * Represents a local assignment: to = from;
 */
public class Assign extends AbstractStatement {

    private final Variable to;

    private final Variable from;

    public Assign(Variable to, Variable from) {
        this.to = to;
        this.from = from;
    }

    public Variable getTo() {
        return to;
    }

    public Variable getFrom() {
        return from;
    }

    @Override
    public void accept(StatementVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return to + " = " + from;
    }
}
