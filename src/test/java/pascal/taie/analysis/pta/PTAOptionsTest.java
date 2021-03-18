/*
 * Tai-e: A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Tai-e is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.taie.analysis.pta;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;

public class PTAOptionsTest {

    @Test
    public void testHelp() {
        PTAOptions.parse("--help");
        if (PTAOptions.get().shouldShowHelp()) {
            PTAOptions.get().printHelp();
        }
    }

    @Test
    public void testVersion() {
        PTAOptions.parse("-v");
        if (PTAOptions.get().shouldShowVersion()) {
            PTAOptions.get().printVersion();
        }
    }

    @Test
    public void testJDKVersion() {
        PTAOptions.parse("-jdk=8");
        Assert.assertEquals(PTAOptions.get().jdkVersion(), 8);
    }
    @Test
    public void testOptions() {
        PTAOptions.parse("--no-implicit-entries", "-cs", "2-object");
        Assert.assertFalse(PTAOptions.get().analyzeImplicitEntries());
        Assert.assertTrue(PTAOptions.get().isMergeStringBuilders());
        Assert.assertEquals("2-object", PTAOptions.get().getContextSensitivity());
        PTAOptions.parse("--no-merge-string-builders");
        Assert.assertFalse(PTAOptions.get().isMergeStringBuilders());
    }

    @Test
    public void testOutputFile() {
        // Well, README will never be output file,
        // this is just for testing ...
        PTAOptions.parse("-f", "README");
        Assert.assertEquals(new File("README"),
                PTAOptions.get().getOutputFile());
    }

    @Test
    public void testSootArgs() {
        PTAOptions.parse("--no-implicit-entries",
                "-cs", "2-object",
                "--", "-cp", "a/b/c.jar", "Main");
        Assert.assertEquals(3, PTAOptions.get().getSootArgs().length);
    }
}
