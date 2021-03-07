/*
 * Tai-e: A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Tai-e is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.taie.ir.stmt;

import pascal.taie.ir.exp.Var;
import pascal.taie.util.Pair;

import java.util.List;
import java.util.stream.Stream;

/**
 * Representation of switch statement, e.g.,
 * switch (v) {
 *   case 1: ...
 *   case 2: ...
 *   default: ...
 * }.
 */
public abstract class SwitchStmt extends JumpStmt {

    protected final Var value;

    protected List<Stmt> targets;

    protected Stmt defaultTarget;

    public SwitchStmt(Var value) {
        this.value = value;
    }

    public Var getValue() {
        return value;
    }

    public Stmt getTarget(int index) {
        return targets.get(index);
    }

    public List<Stmt> getTargets() {
        return targets;
    }

    public void setTargets(List<Stmt> targets) {
        this.targets = targets;
    }

    public abstract Stream<Pair<Integer, Stmt>> getCaseTargets();

    public Stmt getDefaultTarget() {
        return defaultTarget;
    }

    public void setDefaultTarget(Stmt defaultTarget) {
        this.defaultTarget = defaultTarget;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getInsnString());
        sb.append('(').append(value).append(')').append(" {\n");
        getCaseTargets().forEach(caseTarget -> {
            int caseValue = caseTarget.getFirst();
            Stmt target = caseTarget.getSecond();
            sb.append("  ").append("case ").append(caseValue)
                    .append(": goto ")
                    .append(toString(target))
                    .append(";\n");
        });
        sb.append("  default: goto ")
                .append(toString(defaultTarget))
                .append(";\n}");
        return sb.toString();
    }

    protected abstract String getInsnString();
}
