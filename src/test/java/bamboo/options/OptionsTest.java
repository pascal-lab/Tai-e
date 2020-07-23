/*
 * Bamboo - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Bamboo is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package bamboo.options;

import bamboo.pta.options.Options;
import org.junit.Assert;
import org.junit.Test;
import picocli.CommandLine;

public class OptionsTest {

    @Test
    public void testHelp() {
        new CommandLine(new Options()).execute("--help");
    }

    @Test
    public void testVersion() {
        new CommandLine(new Options()).execute("-V");
    }

    @Test
    public void testOptions() {
        new CommandLine(new Options()).execute(
                "--no-implicit-entries",
                "-cs", "2-object");
        Assert.assertFalse(Options.get().analyzeImplicitEntries());
        Assert.assertEquals("2-object", Options.get().getContextSensitivity());
    }

    @Test
    public void testSootArgs() {
        new CommandLine(new Options()).execute(
                "--no-implicit-entries",
                "-cs", "2-object",
                "--", "-cp", "a/b/c.jar", "Main");
        Assert.assertEquals(3, Options.get().getSootArgs().length);
    }
}
