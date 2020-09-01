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

package panda.util;

public class AnalysisException extends RuntimeException {

    /**
     * Constructs a new exception.
     */
    public AnalysisException() {
    }

    /**
     * Constructs a new exception.
     */
    public AnalysisException(String msg) {
        super(msg);
    }

    /**
     * Constructs a new exception.
     */
    public AnalysisException(Throwable t) {
        super(t);
    }

    /**
     * Constructs a new exception.
     */
    public AnalysisException(String msg, Throwable t) {
        super(msg, t);
    }
}
