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
 * Represents an array load: to = base[*];
 */
public class ArrayLoad extends AbstractStatement {

    private final Variable to;

    private final Variable base;

    public ArrayLoad(Variable to, Variable base) {
        this.to = to;
        this.base = base;
        base.addArrayLoad(this);
    }

    public Variable getTo() {
        return to;
    }

    public Variable getBase() {
        return base;
    }

    @Override
    public void accept(StatementVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return to + " = " + base + "[*]";
    }
}
