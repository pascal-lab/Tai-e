/*
 * Bamboo - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Bamboo is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package bamboo.pta;

import bamboo.pta.jimple.JimpleProgramManager;
import bamboo.pta.options.Options;
import soot.PackManager;
import soot.Scene;
import soot.Transform;

public class Main {

    public static void main(String[] args) {
        // Set options
        soot.options.Options.v().set_output_format(
                soot.options.Options.output_format_jimple);
        soot.options.Options.v().set_keep_line_number(true);
        soot.options.Options.v().set_prepend_classpath(true);
        soot.options.Options.v().set_whole_program(true);
        soot.options.Options.v().setPhaseOption("cg", "enabled:false");

        // Configure transformer
        Transform transform = new Transform(
                "wjtp.pta", PointerAnalysisTransformer.v());
        PackManager.v()
                .getPack("wjtp")
                .add(transform);

        // Configure Bamboo options
        Options.parse(args);
        if (Options.get().shouldShowHelp()) {
            Options.get().printHelp();
            return;
        } else if (Options.get().shouldShowVersion()) {
            Options.get().printVersion();
            return;
        }

        JimpleProgramManager.initSoot(Scene.v());

        // Run main analysis
        soot.Main.main(Options.get().getSootArgs());
    }
}
