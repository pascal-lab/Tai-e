/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2022 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2022 Yue Li <yueli@nju.edu.cn>
 *
 * This file is part of Tai-e.
 *
 * Tai-e is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Tai-e is distributed in the hope that it will be useful,but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Tai-e. If not, see <https://www.gnu.org/licenses/>.
 */

package pascal.taie.ir.stmt;

import pascal.taie.ir.exp.ConditionExp;
import pascal.taie.ir.exp.RValue;
import pascal.taie.util.collection.ArraySet;

import java.util.List;
import java.util.Set;

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
    public Set<RValue> getUses() {
        Set<RValue> uses = new ArraySet<>(condition.getUses());
        uses.add(condition);
        return uses;
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
