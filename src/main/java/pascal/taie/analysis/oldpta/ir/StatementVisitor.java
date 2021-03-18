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
