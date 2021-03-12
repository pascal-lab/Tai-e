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

import java.util.stream.IntStream;
import java.util.stream.Stream;

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
    public Stream<Pair<Integer, Stmt>> getCaseTargets() {
        return IntStream.range(lowIndex, highIndex +  1)
                .mapToObj(i -> new Pair<>(i,
                        targets == null ? null : targets.get(i - lowIndex)));
    }

    @Override
    public void accept(StmtVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String getInsnString() {
        return "tableswitch";
    }
}
