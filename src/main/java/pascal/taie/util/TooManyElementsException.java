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

/**
 * Thrown by fixed-capacity collections to indicate that the number of
 * elements added to the collection exceeds its fixed capacity.
 */
public class TooManyElementsException extends RuntimeException {

    /**
     * Constructs a new exception.
     */
    public TooManyElementsException() {
    }

    /**
     * Constructs a new exception.
     */
    public TooManyElementsException(String msg) {
        super(msg);
    }
}
