/*
 * Tai'e - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Tai'e is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.taie.pta;

import pascal.taie.pta.jimple.JimpleProgramManager;
import pascal.taie.pta.options.Options;
import soot.PackManager;
import soot.Scene;
import soot.Transform;

public class Main {

    public static void main(String[] args) {
        // Configure Tai'e options
        Options.parse(args);
        if (Options.get().shouldShowHelp()) {
            Options.get().printHelp();
            return;
        } else if (Options.get().shouldShowVersion()) {
            Options.get().printVersion();
            return;
        }
        JimpleProgramManager.initSoot(Scene.v());

        // Set Soot options
        soot.options.Options.v().set_output_format(
                soot.options.Options.output_format_jimple);
        soot.options.Options.v().set_keep_line_number(true);
        if (!containsJDK(Options.get().getSootArgs())) {
            soot.options.Options.v().set_prepend_classpath(true);
        }
        soot.options.Options.v().set_whole_program(true);
        soot.options.Options.v().setPhaseOption("cg", "enabled:false");

        // Configure Soot transformer
        Transform transform = new Transform(
                "wjtp.pta", PointerAnalysisTransformer.v());
        PackManager.v()
                .getPack("wjtp")
                .add(transform);

        // Run main analysis
        soot.Main.main(Options.get().getSootArgs());
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
