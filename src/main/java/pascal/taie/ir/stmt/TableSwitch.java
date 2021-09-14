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

import pascal.taie.ir.exp.Var;
import pascal.taie.util.collection.Pair;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TableSwitch extends SwitchStmt {

    private final int lowIndex;

    private final int highIndex;

    public TableSwitch(Var value, int lowIndex, int highIndex) {
        super(value);
        this.lowIndex = lowIndex;
        this.highIndex = highIndex;
    }

    public int getLowIndex() {
        return lowIndex;
    }

    public int getHighIndex() {
        return highIndex;
    }

    @Override
    public List<Integer> getCaseValues() {
        return IntStream.range(lowIndex, highIndex + 1)
                .boxed()
                .collect(Collectors.toList());
    }

    @Override
    public List<Pair<Integer, Stmt>> getCaseTargets() {
        return IntStream.range(lowIndex, highIndex + 1)
                .mapToObj(i -> new Pair<>(i,
                        targets == null ? null : targets.get(i - lowIndex)))
                .collect(Collectors.toList());
    }

    @Override
    public <T> T accept(StmtVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String getInsnString() {
        return "tableswitch";
    }
}
