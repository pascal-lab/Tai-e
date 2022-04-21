/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2022 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2022 Yue Li <yueli@nju.edu.cn>
 *
 * This file is part of Tai-e.
 *
 * Tai-e is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Tai-e is distributed in the hope that it will be useful,but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Tai-e. If not, see <https://www.gnu.org/licenses/>.
 */

/**
 * This package provides functionality to analyze exceptions.
 * <p>
 * Here we use the term "exception" by convention, actually, we handle
 * all subclasses of {@link java.lang.Throwable}, including both
 * {@link java.lang.Exception} and {@link java.lang.Error}.
 * <p>
 * We classify exceptions into four categories:
 * <p>
 * (1) VM errors, i.e., subclasses of {@link java.lang.VirtualMachineError}
 * defined below:
 * {@link java.lang.InternalError}
 * {@link java.lang.OutOfMemoryError}
 * {@link java.lang.StackOverflowError}
 * {@link java.lang.UnknownError}
 * According to JVM Spec., Chapter 6.3, the above errors may be thrown
 * at any time during the operation of the Java Virtual Machine.
 * <p>
 * (2) Exceptions that may be implicitly thrown by JVM when executing
 * each instruction. See JVM Spec., Chapter 6.5 for more details.
 * <p>
 * (3) Exceptions that are explicitly thrown by throw statements.
 * <p>
 * (4) Exceptions that are explicitly thrown by method invocations.
 * <p>
 * Generally, Tai-e ignores (1), and provides different strategies
 * to handle exceptions in (2)-(4).
 */
package pascal.taie.analysis.exception;
