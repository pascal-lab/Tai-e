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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OptionsTest {

    private static final String LEGACY_WORLD_BUILDER_CLASS =
            "pascal.taie.frontend.soot.SootWorldBuilder";

    @Test
    void testHelp() {
        Options options = Options.parse("--help");
        if (options.isPrintHelp()) {
            options.printHelp();
        }
    }

    @Test
    void testJavaVersion() {
        Options options = Options.parse("-java=8");
        assertEquals(8, options.getJavaVersion());
    }

    @Test
    void testUseCurrentJRE() {
        Options options = Options.parse();
        assertEquals(Options.getCurrentJavaVersion(),
                options.getJavaVersion());
    }

    @Test
    void testUseCurrentJRERoundTrip(@TempDir Path tempDir) throws IOException {
        Path outputDir = Path.of("build", "tmp", "OptionsTest",
                tempDir.getFileName().toString(), "output-current-jre");
        Options options = Options.parse("--output-dir", outputDir.toString());
        assertTrue(options.isUseCurrentJRE());
        assertEquals(Options.getCurrentJavaVersion(), options.getJavaVersion());

        Path optionsFile = outputDir.resolve("options.yml");
        assertTrue(Files.readString(optionsFile).contains("useCurrentJRE: true"));
        Options replayed = Options.parse("--options-file", optionsFile.toString());
        assertTrue(replayed.isUseCurrentJRE());
        assertEquals(Options.getCurrentJavaVersion(), replayed.getJavaVersion());
    }

    @Test
    void testJavaVersionRoundTrip(@TempDir Path tempDir) throws IOException {
        Path outputDir = Path.of("build", "tmp", "OptionsTest",
                tempDir.getFileName().toString(), "output-java-version");
        Options options = Options.parse("-java", "8", "--output-dir", outputDir.toString());
        assertFalse(options.isUseCurrentJRE());
        assertEquals(8, options.getJavaVersion());

        Path optionsFile = outputDir.resolve("options.yml");
        assertTrue(Files.readString(optionsFile).contains("useCurrentJRE: false"));
        Options replayed = Options.parse("--options-file", optionsFile.toString());
        assertFalse(replayed.isUseCurrentJRE());
        assertEquals(8, replayed.getJavaVersion());
    }

    @SuppressWarnings("deprecation")
    @Test
    void testDeprecatedCompatibilityOptions() {
        Options options = Options.parse("-pp", "-ap");
        assertTrue(options.isPrependJVM());
        assertTrue(options.isAllowPhantom());
        assertTrue(options.isUseCurrentJRE());
        assertEquals(Options.getCurrentJavaVersion(),
                options.getJavaVersion());
    }

    @Test
    void testPrependJVMConflictsWithJavaOptions() {
        assertThrows(ConfigException.class, () -> Options.parse("-java", "8", "-pp"));
        assertThrows(ConfigException.class, () -> Options.parse(
                "--jre-dir", "ignored",
                "-pp"));
    }

    @SuppressWarnings("deprecation")
    @Test
    void testDeprecatedCompatibilityOptionsFile(@TempDir Path tempDir)
            throws IOException {
        Path optionsFile = tempDir.resolve("options.yml");
        Path outputDir = tempDir.resolve("output");
        Files.writeString(optionsFile, """
                prependJVM: true
                allowPhantom: true
                outputDir: "%s"
                """.formatted(outputDir.toString().replace("\\", "\\\\")));
        Options options = Options.parse("--options-file", optionsFile.toString());
        assertTrue(options.isPrependJVM());
        assertTrue(options.isAllowPhantom());
        assertTrue(options.isUseCurrentJRE());
        assertEquals(Options.getCurrentJavaVersion(),
                options.getJavaVersion());
    }

    @Test
    void testMainClass() {
        Options options = Options.parse("-cp", "path/to/cp", "-m", "Main");
        assertEquals("Main", options.getMainClass());
    }

    @Test
    void testAnalyses() {
        Options options = Options.parse(
                "-a", "cfg=exception:true;scope:inter",
                "-a", "pta=timeout:1800;merge-string-objects:false;cs:2-obj",
                "-a", "throw");
        List<PlanConfig> configs = PlanConfig.readConfigs(options);
        PlanConfig cfg = configs.get(0);
        assertTrue((Boolean) cfg.getOptions().get("exception"));
        assertEquals("inter", cfg.getOptions().get("scope"));
        PlanConfig pta = configs.get(1);
        assertEquals(1800, pta.getOptions().get("timeout"));
        assertFalse((Boolean) pta.getOptions().get("merge-string-objects"));
    }

    @Test
    void testKeepResult() {
        Options options = Options.parse();
        assertEquals(Set.of(Plan.KEEP_ALL), options.getKeepResult());
        options = Options.parse("-kr", "pta,def-use");
        assertEquals(Set.of("pta", "def-use"), options.getKeepResult());
    }

    @Test
    void testClasspath() {
        Options options = Options.parse(
                "-cp", ".\\a.jar",
                "-cp", "./dir\\b",
                "-cp", "./c" + File.pathSeparator + "d.jar",
                "-cp", "e.jar" + File.pathSeparator
        );
        assertEquals(List.of(".\\a.jar", "./dir\\b", "./c", "d.jar", "e.jar"),
                options.getClassPath());
    }

    @Test
    void testDeprecatedLegacyWorldBuilder() {
        ByteArrayOutputStream logOutput = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        try (PrintStream capturingOut =
                     new PrintStream(logOutput, true, StandardCharsets.UTF_8)) {
            System.setOut(capturingOut);
            LoggerConfigs.reconfigure();
            Options options = Options.parse(
                    "--world-builder", LEGACY_WORLD_BUILDER_CLASS);
            assertEquals(LEGACY_WORLD_BUILDER_CLASS,
                    options.getWorldBuilderClass().getName());
            String log = logOutput.toString(StandardCharsets.UTF_8);
            assertTrue(log.contains(
                    "DEPRECATED OPTION: Please stop using the legacy frontend"));
        } finally {
            System.setOut(originalOut);
            LoggerConfigs.reconfigure();
        }
    }

}
