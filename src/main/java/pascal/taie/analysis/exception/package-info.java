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

/**
 * This package provides functionality to analyze exceptions.
 *
 * Here we use the term "exception" by convention, actually, we handle
 * all subclasses of {@link java.lang.Throwable}, including both
 * {@link java.lang.Exception} and {@link java.lang.Error}.
 *
 * We classify exceptions into four categories:
 *
 * (1) VM errors, i.e., subclasses of {@link java.lang.VirtualMachineError}
 * defined below:
 * {@link java.lang.InternalError}
 * {@link java.lang.OutOfMemoryError}
 * {@link java.lang.StackOverflowError}
 * {@link java.lang.UnknownError}
 * According to JVM Spec., Chapter 6.3, the above errors may be thrown
 * at any time during the operation of the Java Virtual Machine.
 *
 * (2) Exceptions that may be implicitly thrown by JVM when executing
 * each instruction. See JVM Spec., Chapter 6.5 for more details.
 *
 * (3) Exceptions that are explicitly thrown by throw statements.
 *
 * (4) Exceptions that are explicitly declared by methods.
 *
 * Generally, Tai-e ignores (1), and provides different strategies
 * to handle exceptions in (2)-(4).
 */
package pascal.taie.analysis.exception;
