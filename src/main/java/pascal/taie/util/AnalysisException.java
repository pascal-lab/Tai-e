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

package pascal.taie.util;

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
