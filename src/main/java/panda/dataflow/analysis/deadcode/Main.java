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

package panda.dataflow.analysis.deadcode;

import panda.dataflow.analysis.constprop.ConstantPropagation;
import panda.dataflow.analysis.livevar.LiveVariableAnalysis;
import soot.Pack;
import soot.PackManager;
import soot.Transform;
import soot.options.Options;
import soot.util.PhaseDumper;

import java.util.Collections;

public class Main {

    public static void main(String[] args) {
        // Set options
        Options.v().set_src_prec(Options.src_prec_java);
        Options.v().set_output_format(Options.output_format_jimple);
        Options.v().set_keep_line_number(true);
        Options.v().set_prepend_classpath(true);
        Options.v().set_dump_cfg(Collections.singletonList("jtp.deadcode"));
        PhaseDumper.v().dumpBefore("jtp.deadcode");

        // Configure transformer
        Pack jtp = PackManager.v().getPack("jtp");
        jtp.add(new Transform("jtp.constprop", ConstantPropagation.v()));
        jtp.add(new Transform("jtp.livevar", LiveVariableAnalysis.v()));
        jtp.add(new Transform("jtp.deadcode", DeadCodeDetection.v()));

        // Run main analysis
        soot.Main.main(args);
    }
}
