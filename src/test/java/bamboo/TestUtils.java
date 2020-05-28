/*
 * Bamboo - A Program Analysis Framework for Java
 *
 * Copyright (C)  2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C)  2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Bamboo is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package bamboo;

import org.junit.Assert;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

public class TestUtils {
    public static void testCP(String inputClass) {
        test(inputClass, "constprop",
                "bamboo.dataflow.analysis.constprop.ResultChecker");
    }

    public static void testDCD(String inputClass) {
        test(inputClass, "deadcode",
                "bamboo.dataflow.analysis.deadcode.ResultChecker");
    }

    public static void testCHA(String inputClass) {
        test(inputClass, "cha",
                "bamboo.callgraph.cha.ResultChecker");
    }

    public static void testCIPTA(String inputClass) {
        test(inputClass, "cipta",
                "bamboo.pta.analysis.ci.ResultChecker");
    }

    private static void test(String inputClass, String analysis, String checker) {
        String cp;
        if (new File("analyzed/" + analysis).exists()) {
            cp = "analyzed/" + analysis + "/";
        } else {
            cp = "analyzed/";
        }
        try {
            Class<?> c = Class.forName(checker);
            Method check = c.getMethod("check", String[].class, String.class);
            @SuppressWarnings("unchecked")
            Set<String> mismatches = (Set<String>) check.invoke(null,
                    new String[]{ "-cp", cp, inputClass },
                    cp + inputClass + "-expected.txt");
            Assert.assertTrue(String.join("", mismatches), mismatches.isEmpty());
        } catch (ClassNotFoundException
                | NoSuchMethodException
                | IllegalAccessException
                | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
