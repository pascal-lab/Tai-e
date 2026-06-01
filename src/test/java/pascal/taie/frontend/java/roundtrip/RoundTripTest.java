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

package pascal.taie.frontend.java.roundtrip;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import pascal.taie.backend.bytecode.JarTransformer;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the Java frontend by round-tripping small Java programs.
 *
 * <p>Each test case is a Java source file under {@code src/test/resources}.
 * The test compiles it into a JAR, runs the original JAR, transforms the JAR
 * through Tai-e's bytecode backend, and then runs the transformed JAR.
 * The original and transformed outputs must be identical.
 */
public class RoundTripTest {
    private static final Path RESOURCE_DIR = Path.of(
            "src", "test", "resources", "frontend", "roundtrip");

    private static final String JAVA_VERSION = "17";

    @ParameterizedTest
    @ValueSource(strings = {
            "Primitive",
            "ControlFlow",
            "Array",
            "Field",
            "Invoke",
            "Exception",
            "Cast",
            "InstanceOf",
            "Literal",
            "Synchronized",
            "String",
            "Lambda",
    })
    void roundTrip(String className, @TempDir Path tempDir) throws IOException {
        Path source = RESOURCE_DIR.resolve(className + ".java");
        Path classesDir = tempDir.resolve("classes");
        Path originalJar = tempDir.resolve(className + "-original.jar");
        Path transformedJar = tempDir.resolve(className + "-transformed.jar");

        compile(source, classesDir);
        createJar(classesDir, originalJar);

        String originalOutput = runMain(originalJar, className);
        transformJar(originalJar, transformedJar);
        String transformedOutput = runMain(transformedJar, className);

        assertFalse(originalOutput.isEmpty(), "The test program should print output");
        assertEquals(originalOutput, transformedOutput);
    }

    /**
     * Compiles the given Java source file into the given output directory.
     */
    private static void compile(Path source, Path classesDir) throws IOException {
        assertTrue(Files.isRegularFile(source), "Test source does not exist: " + source);
        Files.createDirectories(classesDir);

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "Tests must run on a JDK, not a JRE");

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        int exitCode = compiler.run(
                null,
                output,
                output,
                "--release", JAVA_VERSION,
                "-d", classesDir.toString(),
                source.toString());

        assertEquals(0, exitCode, () -> "javac failed for " + source + "\n"
                + output.toString(StandardCharsets.UTF_8));
    }

    /**
     * Creates a JAR file from all class files in the given directory.
     */
    private static void createJar(Path classesDir, Path jar) throws IOException {
        Files.deleteIfExists(jar);

        try (OutputStream output = Files.newOutputStream(jar);
             JarOutputStream jarOutput = new JarOutputStream(output);
             Stream<Path> paths = Files.walk(classesDir)) {
            List<Path> classFiles = paths
                    .filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();

            for (Path classFile : classFiles) {
                String entryName = classesDir.relativize(classFile)
                        .toString()
                        .replace('\\', '/');
                jarOutput.putNextEntry(new JarEntry(entryName));
                Files.copy(classFile, jarOutput);
                jarOutput.closeEntry();
            }
        }
    }

    /**
     * Transforms the original JAR through Tai-e's bytecode backend.
     */
    private static void transformJar(Path originalJar, Path transformedJar) throws IOException {
        Files.deleteIfExists(transformedJar);

        String output = captureOutput(() -> JarTransformer.main(new String[]{
                originalJar.toString(),
                transformedJar.toString(),
                JAVA_VERSION,
        }));

        assertTrue(Files.isRegularFile(transformedJar),
                () -> "Transformed JAR was not created: " + transformedJar
                        + "\nJarTransformer output:\n" + output);
    }

    /**
     * Runs the main class in the given JAR with Java 17.
     */
    private static String runMain(Path jar, String className) throws IOException {
        Process process = new ProcessBuilder(
                javaCommand(),
                "-cp",
                jar.toString(),
                className)
                .redirectErrorStream(true)
                .start();

        String output = new String(
                process.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8);

        int exitCode;
        try {
            exitCode = process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while running " + className, e);
        }

        assertEquals(0, exitCode, () -> "Failed to run " + className
                + " from " + jar + "\n" + output);
        return output;
    }

    /**
     * Captures standard output and error produced by the given action.
     */
    private static String captureOutput(Runnable action) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;

        try (PrintStream captured = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            System.setOut(captured);
            System.setErr(captured);
            action.run();
        } catch (RuntimeException | Error e) {
            throw new AssertionError("JarTransformer failed:\n"
                    + output.toString(StandardCharsets.UTF_8), e);
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }

        return output.toString(StandardCharsets.UTF_8);
    }

    /**
     * Returns a Java executable for running test programs.
     */
    private static String javaCommand() {
        return ProcessHandle.current()
                .info()
                .command()
                .orElseGet(() -> Path.of(
                        System.getProperty("java.home"),
                        "bin",
                        javaExecutableName()).toString());
    }

    /**
     * Returns the platform-specific Java executable name.
     */
    private static String javaExecutableName() {
        return System.getProperty("os.name").toLowerCase().contains("win")
                ? "java.exe"
                : "java";
    }
}
