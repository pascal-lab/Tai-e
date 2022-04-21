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

/**
 * Stmt visitor which may return a result after the visit.
 *
 * @param <T> type of the return value
 */
public interface StmtVisitor<T> {

    default T visit(New stmt) {
        return visitDefault(stmt);
    }

    default T visit(AssignLiteral stmt) {
        return visitDefault(stmt);
    }

    default T visit(Copy stmt) {
        return visitDefault(stmt);
    }

    default T visit(LoadArray stmt) {
        return visitDefault(stmt);
    }

    default T visit(StoreArray stmt) {
        return visitDefault(stmt);
    }

    default T visit(LoadField stmt) {
        return visitDefault(stmt);
    }

    default T visit(StoreField stmt) {
        return visitDefault(stmt);
    }

    default T visit(Binary stmt) {
        return visitDefault(stmt);
    }

    default T visit(Unary stmt) {
        return visitDefault(stmt);
    }

    default T visit(InstanceOf stmt) {
        return visitDefault(stmt);
    }

    default T visit(Cast stmt) {
        return visitDefault(stmt);
    }

    default T visit(Goto stmt) {
        return visitDefault(stmt);
    }

    default T visit(If stmt) {
        return visitDefault(stmt);
    }

    default T visit(TableSwitch stmt) {
        return visitDefault(stmt);
    }

    default T visit(LookupSwitch stmt) {
        return visitDefault(stmt);
    }

    default T visit(Invoke stmt) {
        return visitDefault(stmt);
    }

    default T visit(Return stmt) {
        return visitDefault(stmt);
    }

    default T visit(Throw stmt) {
        return visitDefault(stmt);
    }

    default T visit(Catch stmt) {
        return visitDefault(stmt);
    }

    default T visit(Monitor stmt) {
        return visitDefault(stmt);
    }

    default T visit(Nop stmt) {
        return visitDefault(stmt);
    }

    default T visitDefault(Stmt stmt) {
        return null;
    }
}
