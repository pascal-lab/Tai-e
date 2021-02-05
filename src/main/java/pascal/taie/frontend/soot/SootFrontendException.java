/*
 * Tai-e - A Program Analysis Framework for Java
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

package pascal.taie.frontend.soot;

/**
 * Represents the errors raised during reading program information from soot.
 */
public class SootFrontendException extends RuntimeException {

    /**
     * Constructs a new exception.
     */
    public SootFrontendException() {
    }

    /**
     * Constructs a new exception.
     */
    public SootFrontendException(String msg) {
        super(msg);
    }

    /**
     * Constructs a new exception.
     */
    public SootFrontendException(Throwable t) {
        super(t);
    }

    /**
     * Constructs a new exception.
     */
    public SootFrontendException(String msg, Throwable t) {
        super(msg, t);
    }
}
