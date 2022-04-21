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

package pascal.taie.analysis.pta;

import pascal.taie.Main;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class DaCapoRunner {

    private static final String SEP = File.separator;
    private static final String PATH06 = "java-benchmarks" + SEP + "dacapo-2006";
    private static final Set<String> DACAPO06 = Set.of(
            "antlr", "bloat", "chart", "eclipse", "fop",
            "hsqldb", "jython", "luindex", "lusearch", "pmd", "xalan");

    private static final Set<String> ANALYSES = Set.of(
            "ci", "1-call", "2-type", "2-obj");
    private static String PTA = "ci";
    private static String JDK = "-java=6";

    public static void main(String[] args) {
        DaCapoRunner runner = new DaCapoRunner();
        List<String> benchmarks = new ArrayList<>();
        for (String arg : args) {
            if (ANALYSES.contains(arg)) {
                PTA = arg;
            } else if (arg.startsWith("-java=")) {
                JDK = arg;
            } else if (DACAPO06.contains(arg)) {
                benchmarks.add(arg);
            }
        }
        //runner.warmUpJVM();
        for (String bm : benchmarks) {
            runner.run06(bm);
        }
    }

    private void run06(String benchmark) {
        System.out.println("\nAnalyzing " + benchmark);
        Main.main(compose06Args(benchmark));
    }

    private String[] compose06Args(String benchmark) {
        List<String> args = new ArrayList<>();
        String ptaArg = String.format("pta=merge-string-constants:true;" +
                "merge-string-objects:false;cs:%s;reflection-log:%s",
                PTA, PATH06 + SEP + benchmark + "-refl.log");
        Collections.addAll(args,
                "-a", ptaArg,
                "-a", "may-fail-cast",
                "-a", "poly-call",
                JDK,
                // "--pre-build-ir",
                "-cp", buildCP(benchmark),
                "-m", "Harness");
        if (benchmark.equals("eclipse")) {
            args.add("--allow-phantom");
        }
        return args.toArray(new String[0]);
    }

    private String buildCP(String benchmark) {
        List<String> cp = new ArrayList<>();
        cp.add(PATH06 + SEP + benchmark + ".jar");
        cp.add(PATH06 + SEP + benchmark + "-deps.jar");
        return String.join(File.pathSeparator, cp);
    }

    private void warmUpJVM() {
        System.out.println("Warming up JVM ...");
        for (int i = 0; i < 3; ++i) {
            run06("luindex");
        }
        System.out.println("Finish JVM warmup");
    }
}
