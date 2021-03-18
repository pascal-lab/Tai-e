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

package pascal.taie.frontend.soot;

import soot.G;
import soot.Main;
import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.Transform;
import soot.options.Options;

import java.util.Map;

public class TestUtils {

    private TestUtils() {
    }

    public static void buildWorld(String[] args) {
        G.reset();
        // Set Soot options
        Options.v().set_output_format(
                Options.output_format_jimple);
        Options.v().set_keep_line_number(true);
        Options.v().set_whole_program(true);
        Options.v().set_no_writeout_body_releasing(true);
        Options.v().setPhaseOption("jb", "preserve-source-annotations:true");
        Options.v().setPhaseOption("cg", "enabled:false");

        // Configure Soot transformer
        PackManager.v()
                .getPack("wjtp")
                .add(new Transform("wjtp.Tai-e", new SceneTransformer() {
                    @Override
                    protected void internalTransform(String phaseName, Map<String, String> options) {
                        new SootWorldBuilder(Scene.v()).build();
                    }
                }));

        // Run main analysis
        Main.main(args);
    }
}
