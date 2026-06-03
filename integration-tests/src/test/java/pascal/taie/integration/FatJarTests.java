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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
        ProcessResult result = jarRunner.run("-java", "17");
        // Check if the process ran successfully
        assertTrue(result.isSuccess());
        // Check if the log file is created
        File logFile = new File(tempDir, "output/tai-e.log");
        assertTrue(logFile.exists(), "Log file should be created");
        // Check if the log file is not empty
        String logContent = new String(Files.readAllBytes(logFile.toPath()));
        assertFalse(logContent.isEmpty(), "Log file should not be empty");
        // Check if the log file contains expected content
        assertTrue(result.stdout().contains("Writing log to"));
        assertTrue(logContent.contains("Tai-e Version: "));
        String version = logContent.split("Tai-e Version: ")[1].split("\n")[0].strip();
        assertFalse(version.isEmpty(), "Tai-e version should not be empty");
        assertFalse(version.toLowerCase().contains("unknown"), "Tai-e version should not be unknown");
        String commit = logContent.split("Tai-e Commit: ")[1].split("\n")[0].strip();
        assertFalse(commit.isEmpty(), "Tai-e commit should not be empty");
        assertFalse(commit.toLowerCase().contains("unknown"), "Tai-e commit should not be unknown");
        // Check if the options.yml file is created
        File optionsFile = new File(tempDir, "output/options.yml");
        assertTrue(optionsFile.exists(), "Options file should be created");
        // Check if the options.yml file is not empty
        String optionsContent = new String(Files.readAllBytes(optionsFile.toPath()));
        assertFalse(optionsContent.isEmpty(), "Options file should not be empty");
    }

    @Test
    @DisplayName("Should redirect tai-e.log for repeated process runs")
    void testLogFileRedirectionForRepeatedRuns() throws Exception {
        File firstOutputDir = new File(tempDir, "first-output");
        File secondOutputDir = new File(tempDir, "second-output");

        ProcessResult firstResult = jarRunner.run(
                "--output-dir", firstOutputDir.getAbsolutePath(), "-java", "17");
        ProcessResult secondResult = jarRunner.run(
                "--output-dir", secondOutputDir.getAbsolutePath(), "-java", "17");

        assertTrue(firstResult.isSuccess());
        assertTrue(secondResult.isSuccess());

        File firstLog = new File(firstOutputDir, "tai-e.log");
        File secondLog = new File(secondOutputDir, "tai-e.log");
        assertNotEquals(firstLog.getAbsolutePath(), secondLog.getAbsolutePath());
        assertTrue(firstLog.exists(), "First log file should be created");
        assertTrue(secondLog.exists(), "Second log file should be created");

        String firstLogContent = Files.readString(firstLog.toPath());
        String secondLogContent = Files.readString(secondLog.toPath());
        assertEquals(1, countOccurrences(firstLogContent, "Tai-e Version: "));
        assertEquals(1, countOccurrences(secondLogContent, "Tai-e Version: "));
    }

    @Test
    @DisplayName("Should overwrite tai-e.log for repeated process runs in same output dir")
    void testLogFileOverwriteForRepeatedRunsInSameOutputDir() throws Exception {
        File outputDir = new File(tempDir, "same-output");

        ProcessResult firstResult = jarRunner.run(
                "--output-dir", outputDir.getAbsolutePath(), "-java", "17");
        ProcessResult secondResult = jarRunner.run(
                "--output-dir", outputDir.getAbsolutePath(), "-java", "17");

        assertTrue(firstResult.isSuccess());
        assertTrue(secondResult.isSuccess());

        File logFile = new File(outputDir, "tai-e.log");
        assertTrue(logFile.exists(), "Log file should be created");

        String logContent = Files.readString(logFile.toPath());
        assertEquals(1, countOccurrences(logContent, "Tai-e Version: "));
    }

    private static int countOccurrences(String text, String substring) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(substring, index)) != -1) {
            ++count;
            index += substring.length();
        }
        return count;
    }

}
