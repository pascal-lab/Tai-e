/*
 * Tai-e - A Program Analysis Framework for Java
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

package pascal.taie.frontend.soot;

import org.junit.Test;
import pascal.taie.java.World;
import pascal.taie.java.classes.JClass;
import soot.G;
import soot.Main;
import soot.PackManager;
import soot.SceneTransformer;
import soot.Transform;
import soot.options.Options;

import java.util.Comparator;
import java.util.Map;

public class SootFrontendTest {
    
    @Test
    public void testWorldBuilder() {
        G.reset();
        // Set Soot options
        Options.v().set_output_format(
                Options.output_format_jimple);
        Options.v().set_keep_line_number(true);
        Options.v().set_whole_program(true);
        Options.v().setPhaseOption("cg", "enabled:false");

        // Configure Soot transformer
        PackManager.v()
                .getPack("wjtp")
                .add(new Transform("wjtp.Tai-e", new SceneTransformer() {
                    @Override
                    protected void internalTransform(String phaseName, Map<String, String> options) {
                        new SootWorldBuilder().build();
                        World.get().getClassHierarchy()
                                .getAllClasses()
                                .stream()
                                .sorted(Comparator.comparing(JClass::getName))
                                .forEach(System.out::println);
                    }
                }));

        String[] args = new String[] {
                "-cp",
                "java-benchmarks/jre1.6.0_24/rt.jar;" +
                        "java-benchmarks/jre1.6.0_24/jce.jar;" +
                        "java-benchmarks/jre1.6.0_24/jsse.jar;" +
                        "analyzed/pta",
                "Assign"
        };

        // Run main analysis
        Main.main(args);
    }
}
