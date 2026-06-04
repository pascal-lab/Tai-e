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

package pascal.taie.integration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for running Tai-e CLI commands in tests.
 */
public class CliRunner {

    private static final int DEFAULT_TIMEOUT_SECONDS = 30;

    private final List<String> commandPrefix;

    private final File workingDir;

    public CliRunner(List<String> commandPrefix, File workingDir) {
        this.commandPrefix = List.copyOf(commandPrefix);
        this.workingDir = workingDir;
    }

    public static CliRunner forDistribution(String executablePath, File workingDir) {
        File executable = new File(executablePath);
        if (!executable.exists()) {
            throw new IllegalArgumentException("Executable file does not exist: "
                    + executablePath);
        }
        return new CliRunner(List.of(executable.getAbsolutePath()), workingDir);
    }

    public static CliRunner forJar(String javaCommand, String jarPath, File workingDir) {
        File jarFile = new File(jarPath);
        if (!jarFile.exists()) {
            throw new IllegalArgumentException("JAR file does not exist: " + jarPath);
        }
        return new CliRunner(
                List.of(javaCommand, "-jar", jarFile.getAbsolutePath()),
                workingDir);
    }

    /**
     * Runs Tai-e with given arguments.
     */
    public ProcessResult run(String... args) throws IOException, InterruptedException {
        return run(DEFAULT_TIMEOUT_SECONDS, args);
    }

    /**
     * Runs Tai-e with custom timeout.
     */
    public ProcessResult run(int timeoutSeconds, String... args)
            throws IOException, InterruptedException {
        List<String> command = buildCommand(args);
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(workingDir);
        long startTime = System.currentTimeMillis();
        Process process = pb.start();

        String stdout = new String(process.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8);
        String stderr = new String(process.getErrorStream().readAllBytes(),
                StandardCharsets.UTF_8);
        boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Process timed out after "
                    + timeoutSeconds + " seconds");
        }

        long executionTime = System.currentTimeMillis() - startTime;
        return new ProcessResult(
                process.exitValue(),
                stdout,
                stderr,
                executionTime
        );
    }

    private List<String> buildCommand(String... args) {
        List<String> command = new ArrayList<>(commandPrefix);
        command.addAll(Arrays.asList(args));
        return command;
    }

    public File getWorkingDir() {
        return workingDir;
    }

}
