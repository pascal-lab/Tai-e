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

package pascal.taie.ir;

import pascal.taie.ir.stmt.Catch;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.java.types.ClassType;

/**
 * Representation of try-catch block.
 */
public class TryCatchBlock {

    /**
     * The beginning of the try-block (inclusive).
     */
    private final Stmt tryBegin;

    /**
     * The end of the try-block (exclusive).
     */
    private final Stmt tryEnd;

    /**
     * The beginning of the catch-block (inclusive), i.e., the catch statement,
     * which is the handler for the exceptions thrown by the try-block.
     */
    private final Catch catchBegin;

    /**
     * The type of exceptions handled by the handler.
     */
    private final ClassType exceptionType;

    public TryCatchBlock(Stmt tryBegin, Stmt tryEnd, Catch catchBegin, ClassType exceptionType) {
        this.tryBegin = tryBegin;
        this.tryEnd = tryEnd;
        this.catchBegin = catchBegin;
        this.exceptionType = exceptionType;
    }

    /**
     * @return the beginning of the try-block (inclusive).
     */
    public Stmt getTryBegin() {
        return tryBegin;
    }

    /**
     * @return the end of the try-block (exclusive).
     */
    public Stmt getTryEnd() {
        return tryEnd;
    }

    /**
     * @return the beginning of the catch-block (inclusive), i.e.,
     * the catch statement, which is the handler for the exceptions
     * thrown by the try-block.
     */
    public Catch getCatchBegin() {
        return catchBegin;
    }

    /**
     * @return the type of exceptions handled by the handler.
     */
    public ClassType getExceptionType() {
        return exceptionType;
    }

    @Override
    public String toString() {
        return String.format("try [%d, %d), catch %s at %d",
                tryBegin.getIndex(), tryEnd.getIndex(),
                exceptionType, catchBegin.getIndex());
    }
}
