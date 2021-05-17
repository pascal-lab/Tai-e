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

package pascal.taie.ir.stmt;

import pascal.taie.ir.exp.ConditionExp;
import pascal.taie.ir.exp.Exp;
import pascal.taie.util.collection.ListUtils;

import java.util.List;
import java.util.stream.Stream;

/**
 * Representation of if statement, e.g., if a == b goto L;
 */
public class If extends JumpStmt {

    private final ConditionExp condition;

    private Stmt target;

    public If(ConditionExp condition) {
        this.condition = condition;
    }

    public ConditionExp getCondition() {
        return condition;
    }

    public Stmt getTarget() {
        return target;
    }

    public void setTarget(Stmt target) {
        this.target = target;
    }

    @Override
    public List<Exp> getUses() {
        return ListUtils.append(condition.getUses(), condition);
    }

    @Override
    public boolean canFallThrough() {
        return true;
    }

    @Override
    public Stream<Stmt> targets() {
        return Stream.of(target);
    }

    @Override
    public void accept(StmtVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public <T> T accept(StmtRVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return String.format(
                "if (%s) goto %s", condition, toString(target));
    }
}
