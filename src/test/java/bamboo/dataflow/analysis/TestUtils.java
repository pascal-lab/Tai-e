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

package bamboo.dataflow.analysis;

import bamboo.dataflow.analysis.constprop.ResultChecker;
import org.junit.Assert;

import java.io.File;
import java.util.Set;

public class TestUtils {
    public static void testCP(String className) {
        test(className, "constprop/");
    }

    public static void testDCE(String className) {
        test(className, "deadcode/");
    }

    private static void test(String className, String folder) {
        String cp;
        if (new File("analyzed/" + folder).exists()) {
            cp = "analyzed/" + folder;
        } else {
            cp = "analyzed/";
        }
        Set<String> mismatches = ResultChecker.check(
                new String[]{ "-cp", cp, className },
                cp + className + "-expected.txt"
        );
        Assert.assertTrue(String.join("", mismatches), mismatches.isEmpty());
    }
}
