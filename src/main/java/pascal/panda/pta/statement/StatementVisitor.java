/*
 * Panda - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Panda is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.panda.pta.statement;

public interface StatementVisitor {

    default void visit(Allocation alloc) {
    }

    default void visit(ArrayLoad load) {
    }

    default void visit(ArrayStore store) {
    }

    default void visit(Assign assign) {
    }

    default void visit(AssignCast cast) {
    }

    default void visit(Call call) {
    }

    default void visit(InstanceLoad load) {
    }

    default void visit(InstanceStore store) {
    }

    default void visit(StaticLoad load) {
    }

    default void visit(StaticStore store) {
    }
}
