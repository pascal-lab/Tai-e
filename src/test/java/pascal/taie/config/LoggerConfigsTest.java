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

package pascal.taie.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pascal.taie.Main;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LoggerConfigsTest {

    private static final Logger logger = LoggerFactory.getLogger(LoggerConfigsTest.class);

    @Test
    void testSetOutputAndReconfigure(@TempDir Path outputDir) throws IOException {
        String beforeReconfigure = "message written before reconfigure";
        String afterReconfigure = "message written after reconfigure";

        LoggerConfigs.setOutput(outputDir.toFile());
        logger.info(beforeReconfigure);
        LoggerConfigs.reconfigure();
        logger.info(afterReconfigure);

        Path logFile = outputDir.resolve("tai-e.log");
        assertTrue(Files.exists(logFile));
        String log = Files.readString(logFile);
        assertTrue(log.contains(beforeReconfigure));
        assertFalse(log.contains(afterReconfigure));
    }

    @Test
    void testLogRedirectionForRepeatedMainRuns(@TempDir Path tempDir)
            throws IOException {
        Path firstOutputDir = tempDir.resolve("first-output");
        Path secondOutputDir = tempDir.resolve("second-output");

        Main.main(
                "--output-dir", firstOutputDir.toString(),
                "-cp", "src/test/resources/cha",
                "-m", "StaticCall",
                "-a", "cg=algorithm:cha");
        Main.main(
                "--output-dir", secondOutputDir.toString(),
                "-cp", "src/test/resources/cha",
                "-m", "StaticCall",
                "-a", "cg=algorithm:cha");

        Path firstLog = firstOutputDir.resolve("tai-e.log");
        Path secondLog = secondOutputDir.resolve("tai-e.log");
        assertNotEquals(firstLog.toFile().getAbsolutePath(),
                secondLog.toFile().getAbsolutePath());
        assertTrue(Files.exists(firstLog));
        assertTrue(Files.exists(secondLog));

        String firstLogContent = Files.readString(firstLog);
        String secondLogContent = Files.readString(secondLog);
        assertEquals(1, countOccurrences(firstLogContent, "Tai-e Version: "));
        assertEquals(1, countOccurrences(secondLogContent, "Tai-e Version: "));
    }

    @Test
    void testRepeatedMainRunsOverwriteSameLogFile(@TempDir Path outputDir)
            throws IOException {
        Main.main(
                "--output-dir", outputDir.toString(),
                "-cp", "src/test/resources/cha",
                "-m", "StaticCall",
                "-a", "cg=algorithm:cha");
        Main.main(
                "--output-dir", outputDir.toString(),
                "-cp", "src/test/resources/cha",
                "-m", "StaticCall",
                "-a", "cg=algorithm:cha");

        Path logFile = outputDir.resolve("tai-e.log");
        assertTrue(Files.exists(logFile));

        String logContent = Files.readString(logFile);
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
