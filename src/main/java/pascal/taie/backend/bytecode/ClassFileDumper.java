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
 * A utility class that handles the conversion of {@link JClass} and
 * its internal IR into Java bytecode (.class files).
 */
public class ClassFileDumper {

    /**
     * Dumps a {@link JClass} to a .class file.
     *
     * @param jClass  the class to dump
     * @param cp      the classpath to dump the class file to
     * @param version the class file version
     */
    public static void dump(JClass jClass, Path cp, int version) {
        byte[] content = new BytecodeEmitter().emit(jClass, version);
        Path classFilePath = cp.resolve(
                BytecodeEmitter.computeInternalName(jClass) + ".class");
        try {
            Files.createDirectories(classFilePath.getParent());
            Files.write(classFilePath, content);
        } catch (IOException e) {
            throw new RuntimeException("Error: Cannot write class file to " + classFilePath, e);
        }
    }
}
