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

package pascal.taie.frontend.newfrontend.exception;

import pascal.taie.frontend.newfrontend.main.TaiePhase;

/**
 * Custom exception class for Javac-related errors.
 */
public final class JavacException extends FrontendException {

    /**
     * Constructs a JavacException with a default error message.
     * This exception is typically thrown when the Java Development Kit (JDK) is not installed.
     */
    public JavacException() {
        super(TaiePhase.CLOSED_WORLD_ANALYSIS, "Failed to obtain Javac instance. Please ensure the Java Development Kit (JDK) is installed.");
    }

    /**
     * Constructs a JavacException with a custom compile error message.
     *
     * @param compileError the error message from the Javac compiler
     */
    public JavacException(String compileError) {
        super(TaiePhase.CLOSED_WORLD_ANALYSIS, compileError);
    }
}
