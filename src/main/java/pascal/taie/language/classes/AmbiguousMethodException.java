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

package pascal.taie.language.classes;

/**
 * Exception that is thrown when a method is accessed through an ambiguous name.
 */
public class AmbiguousMethodException extends RuntimeException {

    public AmbiguousMethodException(String className, String methodName) {
        super(String.format("%s has multiple methods with name %s", className, methodName));
    }
}
