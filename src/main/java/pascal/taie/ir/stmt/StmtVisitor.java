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

public interface StmtVisitor {

    default void visit(New stmt) {
        visitDefault(stmt);
    }

    default void visit(AssignLiteral stmt) {
        visitDefault(stmt);
    }

    default void visit(Copy stmt) {
        visitDefault(stmt);
    }

    default void visit(LoadArray stmt) {
        visitDefault(stmt);
    }

    default void visit(StoreArray stmt) {
        visitDefault(stmt);
    }

    default void visit(LoadField stmt) {
        visitDefault(stmt);
    }

    default void visit(StoreField stmt) {
        visitDefault(stmt);
    }

    default void visit(Binary stmt) {
        visitDefault(stmt);
    }

    default void visit(Unary stmt) {
        visitDefault(stmt);
    }

    default void visit(InstanceOf stmt) {
        visitDefault(stmt);
    }

    default void visit(Cast stmt) {
        visitDefault(stmt);
    }

    default void visit(Goto stmt) {
        visitDefault(stmt);
    }

    default void visit(If stmt) {
        visitDefault(stmt);
    }

    default void visit(TableSwitch stmt) {
        visitDefault(stmt);
    }

    default void visit(LookupSwitch stmt) {
        visitDefault(stmt);
    }

    default void visit(Invoke stmt) {
        visitDefault(stmt);
    }

    default void visit(Return stmt) {
        visitDefault(stmt);
    }

    default void visit(Throw stmt) {
        visitDefault(stmt);
    }

    default void visit(Catch stmt) {
        visitDefault(stmt);
    }

    default void visit(Monitor stmt) {
        visitDefault(stmt);
    }

    default void visit(Nop stmt) {
        visitDefault(stmt);
    }

    default void visitDefault(Stmt stmt) {
    }
}
