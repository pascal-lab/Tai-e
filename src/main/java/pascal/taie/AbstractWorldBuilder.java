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

package pascal.taie;

import pascal.taie.config.Options;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.natives.DefaultNativeModel;
import pascal.taie.language.natives.EmptyNativeModel;
import pascal.taie.language.natives.NativeModel;
import pascal.taie.language.type.TypeManager;

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
            String jrePath = String.format("%s/jre1.%d",
                    JREs, options.getJavaVersion());
            try (Stream<Path> paths = Files.walk(Path.of(jrePath))) {
                return Stream.concat(
                                paths.map(Path::toString).filter(p -> p.endsWith(".jar")),
                                Stream.of(options.getClassPath()))
                        .collect(Collectors.joining(File.pathSeparator));
            } catch (IOException e) {
                throw new RuntimeException("Analysis on Java " + options.getJavaVersion() +
                        " is not supported yet");
            }
        }
    }

    protected static NativeModel getNativeModel(
            TypeManager typeManager, ClassHierarchy hierarchy) {
        return World.getOptions().enableNativeModel() ?
                new DefaultNativeModel(typeManager, hierarchy) :
                new EmptyNativeModel(typeManager, hierarchy);
    }
}
