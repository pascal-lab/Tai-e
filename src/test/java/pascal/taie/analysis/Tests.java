/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2022 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2022 Yue Li <yueli@nju.edu.cn>
 *
 * This file is part of Tai-e.
 *
 * Tai-e is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Tai-e is distributed in the hope that it will be useful,but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Tai-e. If not, see <https://www.gnu.org/licenses/>.
 */

package pascal.taie.analysis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.Main;
import pascal.taie.World;
import pascal.taie.analysis.graph.cfg.CFGBuilder;
import pascal.taie.analysis.misc.IRDumper;
import pascal.taie.analysis.misc.ResultProcessor;
import pascal.taie.analysis.pta.PointerAnalysis;
import pascal.taie.analysis.pta.plugin.assertion.AssertionChecker;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Static utility methods for testing.
 */
public final class Tests {

    private static final Logger logger = LogManager.getLogger(Tests.class);

    private Tests() {
    }

    /**
     * Whether generate expected results or not.
     */
    private static final boolean GENERATE_EXPECTED_RESULTS = false;

    /**
     * Whether dump IR or not.
     */
    private static final boolean DUMP_IR = false;

    /**
     * Whether dump control-flow graphs or not.
     */
    private static final boolean DUMP_CFG = false;

    /**
     * Starts an analysis for a specific test case.
     * Requires a main method in the given class.
     *
     * @param mainClass the main class to be analyzed
     * @param classPath where the main class is located
     * @param id        ID of the analysis to be executed
     * @param opts      options for the analysis
     */
    public static void testMain(String mainClass, String classPath,
                                String id, String... opts) {
        test(mainClass, true, classPath, id, opts);
    }

    /**
     * Starts an analysis for a specific test case.
     * Do not require a main method in the given class.
     *
     * @param inputClass the input class to be analyzed
     * @param classPath  where the input class is located
     * @param id         ID of the analysis to be executed
     * @param opts       options for the analysis
     */
    public static void testInput(String inputClass, String classPath,
                                 String id, String... opts) {
        test(inputClass, false, classPath, id, opts);
    }

    /**
     * Starts an analysis for a specific test case.
     *
     * @param clz         the class to be analyzed
     * @param isMainClass if the class contains main method
     * @param classPath   where the main class is located
     * @param id          ID of the analysis to be executed
     * @param opts        options for the analysis
     */
    private static void test(String clz, boolean isMainClass,
                             String classPath, String id, String... opts) {
        List<String> args = new ArrayList<>();
        args.add("-pp");
        Collections.addAll(args, "-cp", classPath);
        Collections.addAll(args, isMainClass ? "-m" : "--input-classes", clz);
        if (DUMP_IR) {
            // dump IR
            Collections.addAll(args, "-a", IRDumper.ID);
        }
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
            args.addAll(Arrays.asList(opts).subList(1, opts.length));
        } else {
            Collections.addAll(args, "-a", id);
            Collections.addAll(args, opts);
        }
        // set up result processor
        String action = GENERATE_EXPECTED_RESULTS ? "dump" : "compare";
        String file = getExpectedFile(classPath, clz, id);
        String processArg = String.format("%s=analyses:[%s];action:%s;action-file:%s",
                ResultProcessor.ID, id, action, file);
        Collections.addAll(args, "-a", processArg);
        Main.main(args.toArray(new String[0]));
        if (action.equals("compare")) {
            Set<String> mismatches = World.get().getResult(ResultProcessor.ID);
            assertTrue(mismatches.isEmpty(),
                    "Mismatches of analysis \"" + id + "\":\n" +
                            String.join("\n", mismatches));
        }
    }

    /**
     * @param dir  the directory containing the test case
     * @param main main class of the test case
     * @param id   analysis ID
     * @return the expected file for given test case and analysis.
     */
    private static String getExpectedFile(String dir, String main, String id) {
        String fileName = String.format("%s-%s-expected.txt", main, id);
        return Path.of(dir, fileName).toString();
    }

    public static void testPTA(String dir, String main, String... opts) {
        testPTA(true, dir, main, opts);
    }

    public static void testPTA(boolean processResult,
                               String dir, String main, String... opts) {
        List<String> args = new ArrayList<>();
        args.add("-pp");
        // for loading class PTAAssert
        String ptaTestRoot = "src/test/resources/pta";
        Collections.addAll(args, "-cp", ptaTestRoot);
        // for loading main class
        String classPath = ptaTestRoot + "/" + dir;
        Collections.addAll(args, "-cp", classPath);
        Collections.addAll(args, "-m", main);
        if (DUMP_IR) {
            // dump IR
            Collections.addAll(args, "-a", IRDumper.ID);
        }
        String expectedFile = getExpectedFile(classPath, main, PointerAnalysis.ID);
        String ptaArgs = getPTAArgs(processResult, expectedFile, opts);
        Collections.addAll(args, "-a", PointerAnalysis.ID + "=" + ptaArgs);
        Main.main(args.toArray(new String[0]));
        // move expected file
        if (processResult && GENERATE_EXPECTED_RESULTS) {
            try {
                Path from = new File(World.get().getOptions().getOutputDir(),
                        pascal.taie.analysis.pta.plugin.ResultProcessor.RESULTS_FILE).toPath();
                Files.move(from, Path.of(expectedFile), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                logger.error("Failed to copy expected file", e);
            }
        }
    }

    private static String getPTAArgs(
            boolean processResult, String expectedFile, String... opts) {
        List<String> ptaArgs = new ArrayList<>(List.of(
                "implicit-entries:false",
                "only-app:true",
                "distinguish-string-constants:all"));
        if (processResult) {
            ptaArgs.add(GENERATE_EXPECTED_RESULTS
                    ? "dump:true"
                    : "expected-file:" + expectedFile);
        }
        List<String> plugins = new ArrayList<>();
        plugins.add(AssertionChecker.class.getName());
        for (String opt : opts) {
            if (opt.startsWith("plugins")) {
                // "plugins:[...]"
                String pluginStr = opt.substring(opt.indexOf('[') + 1, opt.indexOf(']'));
                plugins.addAll(Arrays.asList(pluginStr.split(",")));
            } else {
                ptaArgs.add(opt);
            }
        }
        ptaArgs.add("plugins:[" + String.join(",", plugins) + "]");
        return String.join(";", ptaArgs);
    }
}
