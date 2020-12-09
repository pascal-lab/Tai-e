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

package pascal.taie.pta.jimple;

import soot.G;
import soot.Main;
import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.Transform;

import java.util.Map;
import java.util.function.Consumer;

class Utils {

    static void test(String cp, String inputClass, Consumer<IRBuilder> func) {
        // reset Soot
        G.reset();
        // set Soot options
        soot.options.Options.v().set_keep_line_number(true);
        soot.options.Options.v().set_prepend_classpath(true);
        soot.options.Options.v().set_whole_program(true);
        soot.options.Options.v().setPhaseOption("cg", "enabled:false");
        // setup transform
        SceneTransformer transformer = new SceneTransformer() {
            @Override
            protected void internalTransform(String phaseName,
                                             Map<String, String> options) {
                JimpleProgramManager pm = new JimpleProgramManager(Scene.v());
                IRBuilder irBuilder = pm.getIRBuilder();
                func.accept(irBuilder);
            }
        };
        Transform transform = new Transform("wjtp.irbuilder", transformer);
        PackManager.v().getPack("wjtp").add(transform);
        String[] args = new String[] { "-cp", cp, inputClass };
        // run main analysis
        Main.main(args);
    }
}
