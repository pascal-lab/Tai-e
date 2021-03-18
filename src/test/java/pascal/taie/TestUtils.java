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

package pascal.taie;

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
                "pascal.taie.dataflow.analysis.constprop.ResultChecker");
    }

    public static void testDCD(String inputClass) {
        test(inputClass, "deadcode",
                "pascal.taie.dataflow.analysis.deadcode.ResultChecker");
    }

    public static void testCHA(String inputClass) {
        test(inputClass, "cha",
                "pascal.taie.callgraph.cha.ResultChecker");
    }

    public static void testOldPTA(String inputClass) {
        test(inputClass, "pta",
                "pascal.taie.oldpta.core.ci.ResultChecker");
    }

    public static void testOldCSPTA(String inputClass, String... opts) {
        List<String> optList = new ArrayList<>();
        Collections.addAll(optList, opts);
        // ignore implicit entries in test mode
        optList.add("--no-implicit-entries");
        optList.add("--test-mode");
        optList.add("--"); // used by Tai'e to split Soot arguments
        test(inputClass, "cspta",
                "pascal.taie.oldpta.ResultChecker", optList);
    }

    public static void testCSPTA(String inputClass, String... opts) {
        List<String> optList = new ArrayList<>();
        Collections.addAll(optList, opts);
        // ignore implicit entries in test mode
        optList.add("--no-implicit-entries");
        optList.add("--test-mode");
        optList.add("--"); // used by Tai'e to split Soot arguments
        test(inputClass, "cspta",
                "pascal.taie.pta.ResultChecker", optList);
    }

    private static void test(String inputClass, String analysis, String checker) {
        test(inputClass, analysis, checker, Collections.emptyList());
    }

    private static void test(String inputClass, String analysis,
                             String checker, List<String> opts) {
        String cp;
        if (new File("test-resources/" + analysis).exists()) {
            cp = "test-resources/" + analysis + "/";
        } else {
            cp = "test-resources/";
        }
        List<String> args = new ArrayList<>(opts);
        Collections.addAll(args, "-cp", cp, inputClass);
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
