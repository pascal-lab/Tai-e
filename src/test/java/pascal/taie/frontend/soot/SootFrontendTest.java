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

import org.junit.Assert;
import org.junit.Test;
import pascal.taie.java.World;
import pascal.taie.java.classes.JClass;
import pascal.taie.java.classes.JField;
import pascal.taie.java.classes.JMethod;
import pascal.taie.java.classes.Subsignature;
import soot.G;
import soot.Main;
import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
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
                        Scene scene = Scene.v();
                        World.get().getClassHierarchy()
                                .getAllClasses()
                                .stream()
                                .sorted(Comparator.comparing(JClass::getName))
                                .forEach(jclass -> {
                                    SootClass sootClass =
                                            scene.getSootClass(jclass.getName());
                                    examineJClass(jclass, sootClass);
                                });
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

    /**
     * Compare the information of JClass and SootClass.
     * @param jclass
     * @param sootClass
     */
    private void examineJClass(JClass jclass, SootClass sootClass) {
        Assert.assertEquals(jclass.getName(), sootClass.getName());

        if (!jclass.getName().equals("java.lang.Object")) {
            Assert.assertEquals(jclass.getSuperClass().getName(),
                    sootClass.getSuperclass().getName());
        }

        Assert.assertEquals(jclass.getImplementedInterfaces().size(),
                sootClass.getInterfaceCount());

        sootClass.getFields().forEach(sootField -> {
            JField field = jclass.getDeclaredField(sootField.getName());
            Assert.assertEquals(
                    // Soot signatures are quoted, remove quotes for comparison
                    sootField.getSignature().replace("'", ""),
                    field.getSignature());
        });

        sootClass.getMethods().forEach(sootMethod -> {
            // Soot signatures are quoted, remove quotes for comparison
            String sig = sootMethod.getSignature().replace("'", "");
            String subSig = sootMethod.getSubSignature().replace("'", "");
            JMethod method = jclass.getDeclaredMethod(Subsignature.get(subSig));
            Assert.assertEquals(method.getSignature(), sig);
        });
    }
}
