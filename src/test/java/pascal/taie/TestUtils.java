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

package pascal.taie;

import org.junit.Assert;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class TestUtils {
    public static void testCP(String inputClass) {
        test(inputClass, "test-resources/dataflow/constprop/",
                "pascal.taie.analysis.dataflow.clients.constprop.ResultChecker");
    }

    public static void testDCD(String inputClass) {
        test(inputClass, "test-resources/dataflow/deadcode/",
                "pascal.taie.analysis.dataflow.clients.deadcode.ResultChecker");
    }

    public static void testCHA(String inputClass) {
        test(inputClass, "test-resources/cha/",
                "pascal.taie.analysis.graph.callgraph.cha.ResultChecker");
    }

    public static void testOldPTA(String inputClass) {
        test(inputClass, "test-resources/cspta/",
                "pascal.taie.analysis.oldpta.core.ci.ResultChecker");
    }

    public static void testOldCSPTA(String inputClass, String... opts) {
        List<String> optList = new ArrayList<>();
        Collections.addAll(optList, opts);
        // ignore implicit entries in test mode
        optList.add("--no-implicit-entries");
        optList.add("--test-mode");
        optList.add("--"); // used by Tai'e to split Soot arguments
        test(inputClass, "test-resources/pta/cspta/",
                "pascal.taie.analysis.oldpta.ResultChecker", optList);
    }

    public static void testCSPTA(String inputClass, String... opts) {
        List<String> optList = new ArrayList<>();
        // ignore implicit entries in test mode
        String ptaArg = "pta=implicit-entries:false";
        for (String opt : opts) {
            if (opt.startsWith("pta")) {
                ptaArg = opt + ",implicit-entries:false";
            } else {
                optList.add(opt);
            }
        }
        optList.add("-a");
        optList.add(ptaArg);
        optList.add("--test-mode");
        test(inputClass, "test-resources/pta/cspta/",
                "pascal.taie.analysis.pta.ResultChecker", optList);
    }

    private static void test(String inputClass, String analysis, String checker) {
        test(inputClass, analysis, checker, Collections.emptyList());
    }

    private static void test(String inputClass, String classPath,
                             String checker, List<String> opts) {
        List<String> args = new ArrayList<>(opts);
        Collections.addAll(args, "-cp", classPath);
        if (checker.contains(".pta.")) {
            args.add("-m");
        }
        args.add(inputClass);
        try {
            Class<?> c = Class.forName(checker);
            Method check = c.getMethod("check", String[].class, String.class);
            @SuppressWarnings("unchecked")
            Set<String> mismatches = (Set<String>) check.invoke(null,
                    args.toArray(new String[0]),
                    classPath + inputClass + "-expected.txt");
            Assert.assertTrue(String.join("", mismatches), mismatches.isEmpty());
        } catch (ClassNotFoundException
                | NoSuchMethodException
                | IllegalAccessException
                | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
