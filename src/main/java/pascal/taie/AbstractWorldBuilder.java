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

package pascal.taie;

import pascal.taie.config.Options;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.natives.DefaultNativeModel;
import pascal.taie.language.natives.EmptyNativeModel;
import pascal.taie.language.natives.NativeModel;
import pascal.taie.language.type.TypeSystem;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Common functionality for {@link WorldBuilder} implementations.
 */
public abstract class AbstractWorldBuilder implements WorldBuilder {

    protected static final String JREs = "java-benchmarks/JREs";

    protected static final List<String> implicitEntries = List.of(
            "<java.lang.System: void initializeSystemClass()>",
            "<java.lang.Thread: void <init>(java.lang.ThreadGroup,java.lang.Runnable)>",
            "<java.lang.Thread: void <init>(java.lang.ThreadGroup,java.lang.String)>",
            "<java.lang.Thread: void exit()>",
            "<java.lang.ThreadGroup: void <init>()>",
            "<java.lang.ThreadGroup: void <init>(java.lang.ThreadGroup,java.lang.String)>",
            "<java.lang.ThreadGroup: void uncaughtException(java.lang.Thread,java.lang.Throwable)>",
            "<java.lang.ClassLoader: void <init>()>",
            "<java.lang.ClassLoader: java.lang.Class loadClassInternal(java.lang.String)>",
            "<java.lang.ClassLoader: void checkPackageAccess(java.lang.Class,java.security.ProtectionDomain)>",
            "<java.lang.ClassLoader: void addClass(java.lang.Class)>",
            "<java.lang.ClassLoader: long findNative(java.lang.ClassLoader,java.lang.String)>",
            "<java.security.PrivilegedActionException: void <init>(java.lang.Exception)>"
    );

    protected static String getClassPath(Options options) {
        if (options.isPrependJVM()) {
            return options.getClassPath();
        } else { // when prependJVM is not set, we manually specify JRE jars
            // check existence of JREs
            File jreDir = new File(JREs);
            if (!jreDir.exists()) {
                throw new RuntimeException("""
                        Failed to locate Java library.
                        Please clone submodule 'java-benchmarks' by command:
                        git submodule update --init --recursive
                        and put it in Tai-e's working directory.""");
            }
            String jrePath = String.format("%s/jre1.%d",
                    JREs, options.getJavaVersion());
            try (Stream<Path> paths = Files.walk(Path.of(jrePath))) {
                return Stream.concat(
                                paths.map(Path::toString).filter(p -> p.endsWith(".jar")),
                                Stream.of(options.getClassPath()))
                        .collect(Collectors.joining(File.pathSeparator));
            } catch (IOException e) {
                throw new RuntimeException("Analysis on Java " +
                        options.getJavaVersion() + " library is not supported yet", e);
            }
        }
    }

    protected static NativeModel getNativeModel(
            TypeSystem typeSystem, ClassHierarchy hierarchy) {
        return World.get().getOptions().enableNativeModel() ?
                new DefaultNativeModel(typeSystem, hierarchy) :
                new EmptyNativeModel();
    }

    /**
     * @return {@code true} if {@code s} is a path to a file that contains
     * names of input classes.
     */
    protected static boolean isInputClassFile(String s) {
        return s.endsWith(".txt");
    }
}
