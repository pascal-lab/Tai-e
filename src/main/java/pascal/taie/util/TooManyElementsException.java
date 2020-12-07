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
