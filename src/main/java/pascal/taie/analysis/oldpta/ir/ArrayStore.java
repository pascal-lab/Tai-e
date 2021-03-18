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
 * Represents an array store: base[*] = from.
 */
public class ArrayStore extends AbstractStatement {

    private final Variable base;

    private final Variable from;

    public ArrayStore(Variable base, Variable from) {
        this.base = base;
        this.from = from;
        base.addArrayStore(this);
    }

    public Variable getBase() {
        return base;
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
        return base + "[*] = " + from;
    }
}
