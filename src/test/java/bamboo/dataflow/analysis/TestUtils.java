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

import org.junit.Assert;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

public class TestUtils {
    public static void testCP(String className) {
        test(className, "constprop");
    }

    public static void testDCD(String className) {
        test(className, "deadcode");
    }

    private static void test(String className, String analysis) {
        String cp;
        if (new File("analyzed/" + analysis).exists()) {
            cp = "analyzed/" + analysis + "/";
        } else {
            cp = "analyzed/";
        }
        try {
            Class<?> c = Class.forName("bamboo.dataflow.analysis." + analysis + ".ResultChecker");
            Method check = c.getMethod("check", String[].class, String.class);
            @SuppressWarnings("unchecked")
            Set<String> mismatches = (Set<String>) check.invoke(null,
                    new String[]{ "-cp", cp, className },
                    cp + className + "-expected.txt");
            Assert.assertTrue(String.join("", mismatches), mismatches.isEmpty());
        } catch (ClassNotFoundException
                | NoSuchMethodException
                | IllegalAccessException
                | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
