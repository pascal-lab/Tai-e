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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FatJarTests {

    @TempDir
    private static File tempDir;

    private static FatJarRunner jarRunner;

    private static File rootProjectDir;

    @BeforeAll
    static void setupClass() {
        String jarPath = System.getProperty("tai-e.jar.path");
        assertNotNull(jarPath, "System property 'tai-e.jar.path'"
                + " must be set to the path of the fat JAR");
        rootProjectDir = new File(System.getProperty("tai-e.project.path"));
        assertTrue(rootProjectDir.exists() && rootProjectDir.isDirectory(),
                "System property 'tai-e.project.path' must point to the root of the Tai-e project");
        String javaPath = System.getProperty("java.executable.path");
        assertNotNull(javaPath, "System property 'java.executable.path'"
                + " must be set to the path of the Java executable");
        jarRunner = new FatJarRunner(javaPath, jarPath, tempDir);
    }

    @Test
    @DisplayName("Should display usage with no arguments")
    void testNoArguments() throws Exception {
        ProcessResult result = jarRunner.run();
        assertTrue(result.isSuccess());
        assertTrue(result.stdout().contains("Usage: Options"));
    }

    @Test
    @DisplayName("Should display usage with -h")
    void testHelp() throws Exception {
        ProcessResult result = jarRunner.run("-h");
        assertTrue(result.isSuccess());
        assertTrue(result.stdout().contains("Usage: Options"));
    }

    @Test
    @DisplayName("Should handle invalid command")
    void testInvalidCommand() throws Exception {
        ProcessResult result = jarRunner.run("invalid");
        assertFalse(result.isSuccess());
        // TODO: should print usage message
//        assertTrue(result.stdout().contains("Usage: Options"));
    }

    @Test
    @DisplayName("Should write tai-e.log and options.yml files")
    void testLogFile() throws Exception {
        ProcessResult result = jarRunner.run("-pp");
        // Check if the process ran successfully
        assertTrue(result.isSuccess());
        // Check if the log file is created
        File logFile = new File(tempDir, "output/tai-e.log");
        assertTrue(logFile.exists(), "Log file should be created");
        // Check if the log file is not empty
        String logContent = new String(Files.readAllBytes(logFile.toPath()));
        assertFalse(logContent.isEmpty(), "Log file should not be empty");
        // Check if the log file contains expected content
        assertTrue(logContent.contains("Writing log to"));
        assertTrue(result.stdout().contains("Writing log to"));
        // Check if the options.yml file is created
        File optionsFile = new File(tempDir, "output/options.yml");
        assertTrue(optionsFile.exists(), "Options file should be created");
        // Check if the options.yml file is not empty
        String optionsContent = new String(Files.readAllBytes(optionsFile.toPath()));
        assertFalse(optionsContent.isEmpty(), "Options file should not be empty");
    }

}
