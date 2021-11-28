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

import pascal.taie.Main;
import pascal.taie.analysis.misc.ClassDumper;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
     * Whether dump IR or not.
     */
    private static final boolean DUMP_IR = true;

    public static void testCSPTA(String dir, String main, String... opts) {
        doTestPTA("cspta", dir, main, opts);
    }

    private static void doTestPTA(
            String pta, String dir, String main, String... opts) {
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
        if (DUMP_IR) {
            // dump IR
            Collections.addAll(args, "-a", ClassDumper.ID);
        }
        Collections.addAll(args, "-a", pta + "=" + String.join(";", ptaArgs));
        Main.main(args.toArray(new String[0]));
    }
}
