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

import java.util.Collections;
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
abstract class SwitchStmt extends AbstractStmt {

    protected final Var value;

    protected final List<Stmt> targets;

    protected final Stmt defaultTarget;

    public SwitchStmt(Var value, List<Stmt> targets, Stmt defaultTarget) {
        this.value = value;
        this.targets = Collections.unmodifiableList(targets);
        this.defaultTarget = defaultTarget;
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

    public abstract Stream<Pair<Integer, Stmt>> getCaseTargets();

    public Stmt getDefaultTarget() {
        return defaultTarget;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getInsnString());
        sb.append('(').append(value).append(')');
        sb.append('\n').append('{');
        getCaseTargets().forEach(caseTarget -> {
            int caseValue = caseTarget.getFirst();
            Stmt target = caseTarget.getSecond();
            sb.append("  ").append("case ").append(caseValue)
                    .append(": goto ")
                    .append(target.getIndex())
                    .append(";\n");
        });
        sb.append("  default: goto ")
                .append(defaultTarget.getIndex())
                .append(";\n}");
        return sb.toString();
    }

    protected abstract String getInsnString();
}
