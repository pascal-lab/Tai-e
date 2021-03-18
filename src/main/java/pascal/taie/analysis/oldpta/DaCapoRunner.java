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

package pascal.taie.analysis.oldpta;

import soot.G;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DaCapoRunner {

    private static final String SEP = File.separator;
    private static final List<String> BENCHMARK06
            = Arrays.asList("antlr", /*"bloat",*/ "chart", "eclipse", "fop",
            "hsqldb", /*"jython",*/ "luindex", "lusearch", "pmd", "xalan");
    private static final String PATH06 = "java-benchmarks" + SEP + "dacapo-2006";
    private static final String JDK6 = "java-benchmarks" + SEP + "jre1.6.0_24";

    public static void main(String[] args) {
        DaCapoRunner runner = new DaCapoRunner();
        String[] benchmarks;
        if (args.length > 0) {
            benchmarks = args;
        } else {
            benchmarks = BENCHMARK06.toArray(new String[0]);
        }
        //runner.warmUpJVM();
        for (String bm : benchmarks) {
            runner.run06(bm);
        }
    }

    private void run06(String benchmark) {
        G.reset();
        System.out.println("\nAnalyzing " + benchmark);
        Main.main(compose06Args(benchmark));
    }

    private String[] compose06Args(String benchmark) {
        return new String[]{
                "-jdk=6",
                "--merge-string-constants",
                "--pre-build-ir",
                "-cs", "2-obj",
                "--", "-cp", buildCP(JDK6, PATH06, benchmark),
                "dacapo." + benchmark + ".Main"};
    }

    private String buildCP(String jdkPath, String dacapoPath,
                           String benchmark) {
        List<String> cp = new ArrayList<>();
        // setup JDK path
        cp.add(jdkPath + SEP + "rt.jar");
        cp.add(jdkPath + SEP + "jce.jar");
        cp.add(jdkPath + SEP + "jsse.jar");
        cp.add(dacapoPath + SEP + benchmark + ".jar");
        cp.add(dacapoPath + SEP + benchmark + "-deps.jar");
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
