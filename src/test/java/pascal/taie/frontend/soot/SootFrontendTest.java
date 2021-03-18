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
import pascal.taie.World;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.Subsignature;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;

import java.io.File;
import java.util.Comparator;

public class SootFrontendTest {
    
    @Test
    public void testWorldBuilder() {
        String[] args = new String[] {
                "-cp",
                String.join(File.pathSeparator,
                        "java-benchmarks/jre1.6.0_24/rt.jar",
                        "java-benchmarks/jre1.6.0_24/jce.jar",
                        "java-benchmarks/jre1.6.0_24/jsse.jar",
                        "test-resources/cspta"),
                "Assign"
        };
        TestUtils.buildWorld(args);
        World.getClassHierarchy()
                .getAllClasses()
                .stream()
                .sorted(Comparator.comparing(JClass::getName))
                .forEach(jclass -> {
                    SootClass sootClass =
                            Scene.v().getSootClass(jclass.getName());
                    examineJClass(jclass, sootClass);
                });
    }

    /**
     * Compare the information of JClass and SootClass.
     */
    private void examineJClass(JClass jclass, SootClass sootClass) {
        Assert.assertTrue(areSameClasses(jclass, sootClass));

        if (!jclass.getName().equals("java.lang.Object")) {
            Assert.assertTrue(areSameClasses(
                    jclass.getSuperClass(), sootClass.getSuperclass()));
        }

        Assert.assertEquals(jclass.getInterfaces().size(),
                sootClass.getInterfaceCount());

        sootClass.getFields().forEach(sootField -> {
            JField field = jclass.getDeclaredField(sootField.getName());
            Assert.assertTrue(areSameFields(field, sootField));
        });

        sootClass.getMethods().forEach(sootMethod -> {
            // Soot signatures are quoted, remove quotes for comparison
            String subSig = sootMethod.getSubSignature().replace("'", "");
            JMethod method = jclass.getDeclaredMethod(Subsignature.get(subSig));
            Assert.assertTrue(areSameMethods(method, sootMethod));
        });
    }

    private static boolean areSameClasses(JClass jclass, SootClass sootClass) {
        return jclass.getName().equals(sootClass.getName());
    }

    private static boolean areSameFields(JField jfield, SootField sootField) {
        // Soot signatures are quoted, remove quotes for comparison
        return jfield.getSignature()
                .equals(sootField.getSignature().replace("'", ""));
    }

    private static boolean areSameMethods(JMethod jmethod, SootMethod sootMethod) {
        // Soot signatures are quoted, remove quotes for comparison
        return jmethod.getSignature()
                .equals(sootMethod.getSignature().replace("'", ""));
    }
}
