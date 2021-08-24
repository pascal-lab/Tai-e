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
import pascal.taie.analysis.dataflow.analysis.ResultProcessor;
import pascal.taie.analysis.graph.callgraph.CallGraphBuilder;
import pascal.taie.analysis.graph.cfg.CFGBuilder;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Static utility methods for testing.
 */
public final class Tests {

    private Tests() {
    }

    /**
     * Whether generate expected results or not.
     */
    private static final boolean GENERATE_EXPECTED_RESULTS = false;

    /**
     * Whether dump control-flow graphs or not.
     */
    private static final boolean DUMP_CFG = false;

    /**
     * Tests data-flow analysis.
     *
     * @param main      the main class to be analyzed
     * @param classPath where the main class is located
     * @param id        ID of the analysis to be executed
     * @param opts      options for the analysis
     */
    public static void testDFA(String main, String classPath, String id, String... opts) {
        List<String> args = new ArrayList<>();
        args.add("-pp");
        Collections.addAll(args, "-cp", classPath);
        Collections.addAll(args, "-m", main);
        if (DUMP_CFG) {
            // dump control-flow graphs
            Collections.addAll(args, "-a",
                    String.format("%s=dump:true", CFGBuilder.ID));
        }
        // set up the analysis
        if (opts.length > 0 && !opts[0].equals("-a")) {
            // if the opts is not empty, and the opts[0] is not "-a",
            // then this option is given to analysis *id*.
            Collections.addAll(args, "-a", id + "=" + opts[0]);
            for (int i = 1; i < opts.length; ++i) {
                args.add(opts[i]);
            }
        } else {
            Collections.addAll(args, "-a", id);
            Collections.addAll(args, opts);
        }
        // set up result processor
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
        String classPath = "src/test/resources/cha/";
        Collections.addAll(args, "-cp", classPath);
        Collections.addAll(args, "-m", main);
        String action = GENERATE_EXPECTED_RESULTS ? "dump" : "compare";
        String file = Paths.get(classPath, main + "-expected.txt").toString();
        String chaArg = String.format("%s=algorithm:cha;pta:null;action:%s;file:%s",
                CallGraphBuilder.ID, action, file);
        Collections.addAll(args, "-a", chaArg);
        Main.main(args.toArray(new String[0]));
    }

    public static void testPTA(String dir, String main, String... opts) {
        List<String> args = new ArrayList<>();
        args.add("-pp");
        String classPath = "src/test/resources/pta/" + dir;
        Collections.addAll(args, "-cp", classPath);
        Collections.addAll(args, "-m", main);
        List<String> ptaArgs = new ArrayList<>();
        ptaArgs.add("implicit-entries:false");
        String action = GENERATE_EXPECTED_RESULTS ? "dump" : "compare";
        ptaArgs.add("action:" + action);
        String file = Paths.get(classPath, main + "-expected.txt").toString();
        ptaArgs.add("file:" + file);
        boolean specifyOnlyApp = false;
        for (String opt : opts) {
            ptaArgs.add(opt);
            if (opt.contains("only-app")) {
                specifyOnlyApp = true;
            }
        }
        if (!specifyOnlyApp) {
            // if given options do not specify only-app, then set it true
            ptaArgs.add("only-app:true");
        }
        Collections.addAll(args, "-a", "pta=" + String.join(";", ptaArgs));
        Main.main(args.toArray(new String[0]));
    }
}
