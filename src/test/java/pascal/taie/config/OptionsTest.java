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

import java.io.File;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OptionsTest {

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
        assertEquals(options.getJavaVersion(), 8);
    }

    @Test
    void testPrependJVM() {
        Options options = Options.parse("-pp");
        assertEquals(Options.getCurrentJavaVersion(),
                options.getJavaVersion());
    }

    @Test
    void testMainClass() {
        Options options = Options.parse("-cp", "path/to/cp", "-m", "Main");
        assertEquals("Main", options.getMainClass());
    }

    @Test
    void testAllowPhantom() {
        Options options = Options.parse();
        assertFalse(options.isAllowPhantom());
        options = Options.parse("--allow-phantom");
        assertTrue(options.isAllowPhantom());
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

}
