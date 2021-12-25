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

package pascal.taie.ir.proginfo;

import pascal.taie.ir.stmt.Catch;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.type.ClassType;

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
                             Catch handler, ClassType catchType) {

    @Override
    public String toString() {
        return String.format("try [%d, %d), catch %s at %d",
                start.getIndex(), end.getIndex(),
                catchType, handler.getIndex());
    }
}
