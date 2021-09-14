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
import pascal.taie.ir.exp.RValue;
import pascal.taie.util.collection.CollectionUtils;

import java.util.List;

/**
 * Representation of if statement, e.g., if a == b goto S;
 */
public class If extends JumpStmt {

    /**
     * The condition expression.
     */
    private final ConditionExp condition;

    /**
     * Jump target when the condition expression is evaluated to true.
     */
    private Stmt target;

    public If(ConditionExp condition) {
        this.condition = condition;
    }

    /**
     * @return the condition expression of the if-statement.
     */
    public ConditionExp getCondition() {
        return condition;
    }

    /**
     * @return the jump target (when the condition expression is evaluated
     * to true) of the if-statement.
     */
    public Stmt getTarget() {
        return target;
    }

    public void setTarget(Stmt target) {
        this.target = target;
    }

    @Override
    public List<RValue> getUses() {
        return CollectionUtils.append(condition.getUses(), condition);
    }

    @Override
    public List<Stmt> getTargets() {
        return List.of(target);
    }

    @Override
    public <T> T accept(StmtVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return String.format(
                "if (%s) goto %s", condition, toString(target));
    }
}
