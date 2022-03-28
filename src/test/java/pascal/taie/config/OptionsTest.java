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

package pascal.taie.config;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OptionsTest {

    @Test
    public void testHelp() {
        Options options = Options.parse("--help");
        if (options.isPrintHelp()) {
            options.printHelp();
        }
    }

    @Test
    public void testJavaVersion() {
        Options options = Options.parse("-java=8");
        assertEquals(options.getJavaVersion(), 8);
    }

    @Test
    public void testPrependJVM() {
        Options options = Options.parse("-pp");
        assertEquals(Options.getCurrentJavaVersion(),
                options.getJavaVersion());
    }

    @Test
    public void testMainClass() {
        Options options = Options.parse("-cp", "path/to/cp", "-m", "Main");
        assertEquals("Main", options.getMainClass());
    }

    @Test
    public void testAllowPhantom() {
        Options options = Options.parse();
        assertFalse(options.isAllowPhantom());
        options = Options.parse("--allow-phantom");
        assertTrue(options.isAllowPhantom());
    }

    @Test
    public void testAnalyses() {
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
}
