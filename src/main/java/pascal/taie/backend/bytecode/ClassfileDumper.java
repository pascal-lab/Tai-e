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

package pascal.taie.backend.bytecode;

import pascal.taie.language.classes.JClass;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A utility class to dump a classfile to the file system.
 */
public class ClassfileDumper {
    /**
     * Dump a classfile to the file system.
     * @param cp the classpath to dump the classfile to
     * @param jClass the class to dump
     */
    public static void dump(Path cp, JClass jClass) {
        byte[] classfileBuffer = new BytecodeEmitter().emit(jClass);
        Path classfilePath = cp.resolve(
                BytecodeEmitter.computeInternalName(jClass) + ".class");
        try {
            Files.createDirectories(classfilePath.getParent());
            Files.write(classfilePath, classfileBuffer);
        } catch (IOException e) {
            throw new RuntimeException("Error: Cannot write classfile to " + classfilePath, e);
        }
    }
}
