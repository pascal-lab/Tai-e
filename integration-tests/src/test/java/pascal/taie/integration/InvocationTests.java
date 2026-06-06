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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

abstract class InvocationTests {

    private static final String TAI_E_VERSION = "Tai-e Version: ";

    private static final String TAI_E_COMMIT = "Tai-e Commit: ";

    @TempDir
    private File tempDir;

    private CliRunner runner;

    abstract CliRunner createRunner(File workingDir);

    @BeforeEach
    void setup() {
        runner = createRunner(tempDir);
    }

    @Test
    @DisplayName("Should display usage with no arguments")
    void testNoArguments() throws Exception {
        ProcessResult result = runner.run();
        assertTrue(result.isSuccess());
        assertTrue(result.stdout().contains("Usage: Options"));
    }

    @Test
    @DisplayName("Should display usage with -h")
    void testHelp() throws Exception {
        ProcessResult result = runner.run("-h");
        assertTrue(result.isSuccess());
        assertTrue(result.stdout().contains("Usage: Options"));
    }

    @Test
    @DisplayName("Should handle invalid command")
    void testInvalidCommand() throws Exception {
        ProcessResult result = runner.run("invalid");
        assertFalse(result.isSuccess());
        // TODO: should print usage message
        // assertTrue(result.stdout().contains("Usage: Options"));
    }

    @Test
    @DisplayName("Should write tai-e.log and options.yml files")
    void testLogFile() throws Exception {
        ProcessResult result = runner.run("-java", "17");
        assertTrue(result.isSuccess());

        File logFile = new File(tempDir, "output/tai-e.log");
        assertTrue(logFile.exists(), "Log file should be created");
        String logContent = Files.readString(logFile.toPath());
        assertFalse(logContent.isEmpty(), "Log file should not be empty");

        assertTrue(result.stdout().contains("Writing log to"));
        assertBuildInfo(result.stdout(), "stdout");
        assertBuildInfo(logContent, "log file");

        File optionsFile = new File(tempDir, "output/options.yml");
        assertTrue(optionsFile.exists(), "Options file should be created");
        String optionsContent = Files.readString(optionsFile.toPath());
        assertFalse(optionsContent.isEmpty(), "Options file should not be empty");
    }

    @Test
    @DisplayName("Should redirect tai-e.log for repeated process runs")
    void testLogFileRedirectionForRepeatedRuns() throws Exception {
        File firstOutputDir = new File(tempDir, "first-output");
        File secondOutputDir = new File(tempDir, "second-output");

        ProcessResult firstResult = runner.run(
                "--output-dir", firstOutputDir.getAbsolutePath(), "-java", "17");
        ProcessResult secondResult = runner.run(
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
        assertBuildInfo(firstResult.stdout(), "first stdout");
        assertBuildInfo(secondResult.stdout(), "second stdout");
        assertBuildInfo(firstLogContent, "first log file");
        assertBuildInfo(secondLogContent, "second log file");
        assertEquals(1, countLinesContaining(firstResult.stdout(), TAI_E_VERSION));
        assertEquals(1, countLinesContaining(secondResult.stdout(), TAI_E_VERSION));
        assertEquals(1, countLinesContaining(firstLogContent, TAI_E_VERSION));
        assertEquals(1, countLinesContaining(secondLogContent, TAI_E_VERSION));
    }

    @Test
    @DisplayName("Should overwrite tai-e.log for repeated process runs in same output dir")
    void testLogFileOverwriteForRepeatedRunsInSameOutputDir() throws Exception {
        File outputDir = new File(tempDir, "same-output");

        ProcessResult firstResult = runner.run(
                "--output-dir", outputDir.getAbsolutePath(), "-java", "17");
        ProcessResult secondResult = runner.run(
                "--output-dir", outputDir.getAbsolutePath(), "-java", "17");

        assertTrue(firstResult.isSuccess());
        assertTrue(secondResult.isSuccess());

        File logFile = new File(outputDir, "tai-e.log");
        assertTrue(logFile.exists(), "Log file should be created");

        String logContent = Files.readString(logFile.toPath());
        assertBuildInfo(firstResult.stdout(), "first stdout");
        assertBuildInfo(secondResult.stdout(), "second stdout");
        assertBuildInfo(logContent, "log file");
        assertEquals(1, countLinesContaining(firstResult.stdout(), TAI_E_VERSION));
        assertEquals(1, countLinesContaining(secondResult.stdout(), TAI_E_VERSION));
        assertEquals(1, countLinesContaining(logContent, TAI_E_VERSION));
    }

    private static void assertBuildInfo(String text, String source) {
        assertBuildInfoValue(text, source, TAI_E_VERSION, "Tai-e version");
        assertBuildInfoValue(text, source, TAI_E_COMMIT, "Tai-e commit");
    }

    private static void assertBuildInfoValue(
            String text, String source, String prefix, String description) {
        String value = text.lines()
                .filter(line -> line.contains(prefix))
                .findFirst()
                .map(line -> line.substring(
                        line.indexOf(prefix) + prefix.length()).strip())
                .orElse("");
        assertFalse(value.isEmpty(), description + " in " + source
                + " should not be empty");
        assertFalse(value.toLowerCase().contains("unknown"),
                description + " in " + source + " should not be unknown");
    }

    private static long countLinesContaining(String text, String substring) {
        return text.lines()
                .filter(line -> line.contains(substring))
                .count();
    }

}
