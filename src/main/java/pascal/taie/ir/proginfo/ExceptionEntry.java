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
import pascal.taie.language.types.ClassType;

/**
 * Representation of exception entries. Each entry consists of four items:
 * start, end: the ranges in the Stmt array at which the exception
 *  handler is active.
 * handler: start of exception handler.
 * catchType: the class of exceptions that this exception handler
 *  is designated to catch.
 */
public class ExceptionEntry {

    /**
     * The beginning of the try-block (inclusive).
     */
    private final Stmt start;

    /**
     * The end of the try-block (exclusive).
     */
    private final Stmt end;

    /**
     * The beginning of the catch-block (inclusive), i.e., the catch statement,
     * which is the handler for the exceptions thrown by the try-block.
     */
    private final Catch handler;

    /**
     * The type of exceptions handled by the handler.
     */
    private final ClassType catchType;

    public ExceptionEntry(Stmt start, Stmt end, Catch handler, ClassType catchType) {
        this.start = start;
        this.end = end;
        this.handler = handler;
        this.catchType = catchType;
    }

    /**
     * @return the beginning of the try-block (inclusive).
     */
    public Stmt getStart() {
        return start;
    }

    /**
     * @return the end of the try-block (exclusive).
     */
    public Stmt getEnd() {
        return end;
    }

    /**
     * @return the beginning of the catch-block (inclusive), i.e.,
     * the handler for the exceptions thrown by the try-block.
     */
    public Catch getHandler() {
        return handler;
    }

    /**
     * @return the type of exceptions handled by the handler.
     */
    public ClassType getCatchType() {
        return catchType;
    }

    @Override
    public String toString() {
        return String.format("try [%d, %d), catch %s at %d",
                start.getIndex(), end.getIndex(),
                catchType, handler.getIndex());
    }
}
