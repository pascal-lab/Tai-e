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

package pascal.taie.newpta;

import pascal.taie.frontend.soot.SootWorldBuilder;
import pascal.taie.pta.PTAOptions;
import soot.PackManager;
import soot.Transform;
import soot.options.Options;

public class Main {

    public static void main(String[] args) {
        // Configure Tai-e options
        PTAOptions.parse(args);
        if (PTAOptions.get().shouldShowHelp()) {
            PTAOptions.get().printHelp();
            return;
        } else if (PTAOptions.get().shouldShowVersion()) {
            PTAOptions.get().printVersion();
            return;
        }
        SootWorldBuilder.initSoot();

        // Set Soot options
        Options.v().set_output_format(
                Options.output_format_jimple);
        Options.v().set_keep_line_number(true);
        if (!containsJDK(PTAOptions.get().getSootArgs())) {
            Options.v().set_prepend_classpath(true);
        }
        Options.v().set_whole_program(true);
        Options.v().setPhaseOption("cg", "enabled:false");

        // Configure Soot transformer
        Transform transform = new Transform(
                "wjtp.pta", PointerAnalysisTransformer.v());
        PackManager.v()
                .getPack("wjtp")
                .add(transform);

        // Run main analysis
        soot.Main.main(PTAOptions.get().getSootArgs());
    }

    /**
     * Check if Soot arguments contain the class paths for JRE/JDK.
     */
    private static boolean containsJDK(String[] sootArgs) {
        for (String arg : sootArgs) {
            if ((arg.toLowerCase().contains("jre")
                    || arg.toLowerCase().contains("jdk"))
                    && arg.toLowerCase().contains(".jar")) {
                return true;
            }
        }
        return false;
    }
}
