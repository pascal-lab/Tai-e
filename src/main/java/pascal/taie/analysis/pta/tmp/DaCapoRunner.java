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

package pascal.taie.analysis.pta.tmp;

import pascal.taie.Main;
import soot.G;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DaCapoRunner {

    private static final String SEP = File.separator;
    private static final List<String> BENCHMARK06
            = List.of("antlr", /*"bloat",*/ "chart", "eclipse", "fop",
            "hsqldb", /*"jython",*/ "luindex", "lusearch", "pmd", "xalan");
    private static final String PATH06 = "java-benchmarks" + SEP + "dacapo-2006";

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
                "-a", "pta=merge-string-constants:true,cs:2-obj",
                "-java=6",
                "--pre-build-ir",
                "-cp", buildCP(benchmark),
                "-m", "dacapo." + benchmark + ".Main"
        };
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
