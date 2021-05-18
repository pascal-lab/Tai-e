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

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class NewTestUtils {

    /**
     * If generate expected results.
     */
    private static final boolean GENERATE_EXPECTED_RESULTS = false;

    public static void testCP(String inputClass) {
        test(inputClass, "test-resources/dataflow/constprop/",
                ConstantPropagation.ID);
    }

    public static void testDCD(String inputClass) {
        test(inputClass, "test-resources/dataflow/deadcode/",
                DeadCodeDetection.ID);
    }

    private static void test(String main, String classPath, String id) {
        test(main, classPath, id, List.of());
    }

    private static void test(String main, String classPath,
                             String id, List<String> opts) {
        List<String> args = new ArrayList<>(opts);
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
}
