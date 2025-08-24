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
 * Provides classes for generating bytecode from Tai-e IR.
 * This package includes classes responsible for emitting .class files,
 * handling bytecode instructions, and managing class file dumping.
 * <p>
 * The main components are:
 * <ul>
 *   <li>{@link pascal.taie.backend.bytecode.BytecodeEmitter}: Emits bytecode for a given Java class.</li>
 *   <li>{@link pascal.taie.backend.bytecode.ClassFileDumper}: Dumps generated class files to disk.</li>
 *   <li>{@link pascal.taie.backend.bytecode.JarTransformer}: Packs generated class files into a JAR archive.</li>
 * </ul>
 * <p>
 * <p>
 * {@link pascal.taie.backend.bytecode.JarTransformer} provides a command-line interface for packing generated class files into a JAR archive.
 * It can be used as follows:
 * <pre>
 * java -cp tai-e.jar pascal.taie.backend.bytecode.JarDumper <em>input-jar</em> <em>output-jar</em> <em>java-language-level</em>
 * </pre>
 */
package pascal.taie.backend.bytecode;
