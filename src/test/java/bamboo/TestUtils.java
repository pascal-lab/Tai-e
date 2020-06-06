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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

    public static void testPTA(String inputClass) {
        test(inputClass, "pta",
                "bamboo.pta.analysis.ci.ResultChecker");
    }

    public static void testCSPTA(String inputClass, String... opts) {
        test(inputClass, "cspta",
                "bamboo.pta.ResultChecker", opts);
    }

    private static void test(String inputClass, String analysis, String checker) {
        test(inputClass, analysis, checker, new String[0]);
    }

    private static void test(String inputClass, String analysis, String checker, String[] opts) {
        String cp;
        if (new File("analyzed/" + analysis).exists()) {
            cp = "analyzed/" + analysis + "/";
        } else {
            cp = "analyzed/";
        }
        List<String> args = new ArrayList<>();
        Collections.addAll(args, "-cp", cp);
        Collections.addAll(args, opts);
        args.add(inputClass);
        try {
            Class<?> c = Class.forName(checker);
            Method check = c.getMethod("check", String[].class, String.class);
            @SuppressWarnings("unchecked")
            Set<String> mismatches = (Set<String>) check.invoke(null,
                    args.toArray(new String[0]),
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
