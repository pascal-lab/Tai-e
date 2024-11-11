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
 * Exception thrown when a .class file is found to be corrupt during the compilation process.
 */
public final class CorruptClassFileException extends FrontendException {

    /**
     * Constructs a new instance of CorruptClassFileException with the specified phase, binary name, and corruption details.
     *
     * @param phase the phase of the taie where the corruption was detected
     * @param corruption the details of the corruption found in the .class file
     */
    public CorruptClassFileException(TaiePhase phase, ClassFileInfo info, ClassFileCorruption corruption) {
        super(phase, String.format("""
                %s is corrupt.
                Corruption details:
                %s
                This might be a bug in the tai-e frontend. To troubleshoot, try loading the class with a JVM.
                If the JVM can load the class or you believe the class is not corrupt, please submit a bug report at:
                %s""",
                info.toString(), corruption.toString(), FrontendException.TAIE_ISSUES));
    }
}
