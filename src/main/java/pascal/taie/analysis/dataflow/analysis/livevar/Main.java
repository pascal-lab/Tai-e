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

package pascal.taie.analysis.dataflow.analysis.livevar;

import soot.PackManager;
import soot.Transform;
import soot.options.Options;

public class Main {

    public static void main(String[] args) {
        // Set options
        Options.v().set_src_prec(Options.src_prec_java);
        Options.v().set_output_format(Options.output_format_jimple);
        Options.v().set_prepend_classpath(true);

        // Configure transformer
        PackManager.v()
                .getPack("jtp")
                .add(new Transform("jtp.livevar", LiveVariableAnalysis.v()));

        // Run main analysis
        soot.Main.main(args);
    }
}
