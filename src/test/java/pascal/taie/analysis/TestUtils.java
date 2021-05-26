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

package pascal.taie.analysis;

import org.junit.Assert;
import pascal.taie.Main;
import pascal.taie.World;
import pascal.taie.analysis.dataflow.analysis.DeadCodeDetection;
import pascal.taie.analysis.dataflow.analysis.ResultProcessor;
import pascal.taie.analysis.dataflow.analysis.constprop.ConstantPropagation;
import pascal.taie.analysis.graph.callgraph.CallGraphBuilder;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class TestUtils {

    /**
     * If generate expected results.
     */
    private static final boolean GENERATE_EXPECTED_RESULTS = false;

    public static void testCP(String inputClass) {
        testIntra(inputClass, "test-resources/dataflow/constprop/",
                ConstantPropagation.ID);
    }

    public static void testDCD(String inputClass) {
        testIntra(inputClass, "test-resources/dataflow/deadcode/",
                DeadCodeDetection.ID);
    }

    private static void testIntra(String main, String classPath, String id) {
        List<String> args = new ArrayList<>();
        args.add("-pp");
        Collections.addAll(args, "-cp", classPath);
        Collections.addAll(args, "-m", main);
        Collections.addAll(args, "-a", id);
        String action = GENERATE_EXPECTED_RESULTS ? "dump" : "compare";
        String file = Paths.get(classPath, main + "-expected.txt").toString();
        String processArg = String.format("%s=analyses:[%s];action:%s;file:%s",
                ResultProcessor.ID, id, action, file);
        Collections.addAll(args, "-a", processArg);
        Main.main(args.toArray(new String[0]));
        if (action.equals("compare")) {
            Set<String> mismatches = World.getResult(ResultProcessor.ID);
            Assert.assertTrue("Mismatches of analysis \"" + id + "\":\n" +
                            String.join("\n", mismatches),
                    mismatches.isEmpty());
        }
    }

    public static void testCHA(String main) {
        List<String> args = new ArrayList<>();
        args.add("-pp");
        String classPath = "test-resources/cha/";
        Collections.addAll(args, "-cp", classPath);
        Collections.addAll(args, "-m", main);
        String action = GENERATE_EXPECTED_RESULTS ? "dump" : "compare";
        String file = Paths.get(classPath, main + "-expected.txt").toString();
        String chaArg = String.format("%s=algorithm:cha;action:%s;file:%s",
                CallGraphBuilder.ID, action, file);
        Collections.addAll(args, "-a", chaArg);
        Main.main(args.toArray(new String[0]));
    }

    public static void testCSPTA(String main, String... opts) {
        List<String> args = new ArrayList<>();
        String classPath = "test-resources/pta/cspta/";
        Collections.addAll(args, "-cp", classPath);
        Collections.addAll(args, "-m", main);
        String action = GENERATE_EXPECTED_RESULTS ? "dump" : "compare";
        String file = Paths.get(classPath, main + "-expected.txt").toString();
        // ignore implicit entries in test mode
        String suffix = String.format("implicit-entries:false;action:%s;file:%s",
                action, file);
        String ptaArg = "pta=" + suffix;
        for (String opt : opts) {
            if (opt.startsWith("pta")) {
                ptaArg = opt + ";" + suffix;
            } else {
                args.add(opt);
            }
        }
        Collections.addAll(args, "-a", ptaArg);
        Main.main(args.toArray(new String[0]));
    }
}
