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

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Represents an unknown exception that occurred during the execution of the frontend.
 * This exception is thrown when an unexpected error occurs and is not handled by the frontend.
 */
public final class UnknownFrontendException extends FrontendException {
    /**
     * Constructs a new UnknownException with the given phase and the underlying cause.
     *
     * @param phase the phase at which the exception occurred
     * @param e     the underlying cause of the exception
     */
    public UnknownFrontendException(TaiePhase phase, Throwable e) {
        super(phase, String.format("""
                Unexpected error occurred
                %s
                Consider submitting a bug report at %s
                """, printException(e), TAIE_ISSUES));
    }

    /**
     * Prints the stack trace of the given exception in a human-readable format.
     *
     * @param e the exception to print
     * @return the stack trace of the exception as a string
     */
    private static String printException(Throwable e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
