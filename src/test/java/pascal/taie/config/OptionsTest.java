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

import org.junit.Assert;
import org.junit.Test;
import pascal.taie.config.Options;
import pascal.taie.config.PlanConfig;

import java.util.List;

public class OptionsTest {

    @Test
    public void testHelp() {
        Options options = Options.parse("--help");
        if (options.isPrintHelp()) {
            options.printHelp();
        }
    }

    @Test
    public void testVersion() {
        Options options = Options.parse("-v");
        if (options.isPrintVersion()) {
            options.printVersion();
        }
    }

    @Test
    public void testJavaVersion() {
        Options options = Options.parse("-java=8");
        Assert.assertEquals(options.getJavaVersion(), 8);
    }

    @Test
    public void testPrependJVM() {
        Options options = Options.parse("-pp");
        Assert.assertEquals(Options.getCurrentJavaVersion(),
                options.getJavaVersion());
    }

    @Test
    public void testMainClass() {
        Options options = Options.parse("-cp", "path/to/cp", "-m", "Main");
        Assert.assertEquals("Main", options.getMainClass());
    }

    @Test
    public void testOptions2() {
        Options options = Options.parse(
                "-a", "cfg=exception:true,scope:inter",
                "-a", "pta=timeout:1800,merge-string-objects:false,cs:2-obj",
                "-a", "throw");
        List<PlanConfig> configs = PlanConfig.readFromOptions(options);
        PlanConfig cfg = configs.get(0);
        Assert.assertTrue((Boolean) cfg.getOptions().get("exception"));
        Assert.assertEquals("inter", cfg.getOptions().get("scope"));
        PlanConfig pta = configs.get(1);
        Assert.assertEquals(1800, pta.getOptions().get("timeout"));
        Assert.assertFalse((Boolean) pta.getOptions().get("merge-string-objects"));
        PlanConfig throwConfig = configs.get(2);
        Assert.assertTrue(throwConfig.getOptions().isEmpty());
    }
}
