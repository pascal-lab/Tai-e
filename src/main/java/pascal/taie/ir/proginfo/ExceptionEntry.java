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

package pascal.taie.ir.proginfo;

import pascal.taie.ir.stmt.Catch;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.type.ClassType;

import java.io.Serializable;

/**
 * Representation of exception entries. Each entry consists of four items:
 * <ul>
 *     <li>start: the beginning of the try-block (inclusive).
 *     <li>end: the end of the try-block (exclusive).
 *     <li>handler: the beginning of the catch-block (inclusive),
 *     i.e., the handler for the exceptions thrown by the try-block.
 *     <li>catchType: the class of exceptions that this exception handler
 *     is designated to catch.
 * </ul>
 */
public record ExceptionEntry(Stmt start, Stmt end,
                             Catch handler, ClassType catchType)
        implements Serializable {

    @Override
    public String toString() {
        return String.format("try [%d, %d), catch %s at %d",
                start.getIndex(), end.getIndex(),
                catchType, handler.getIndex());
    }
}
