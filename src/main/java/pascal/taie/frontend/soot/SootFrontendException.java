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

package pascal.taie.frontend.soot;

/**
 * Represents the errors raised during reading program information from soot.
 */
class SootFrontendException extends RuntimeException {

    /**
     * Constructs a new exception.
     */
    SootFrontendException(String msg) {
        super(msg);
    }
}
