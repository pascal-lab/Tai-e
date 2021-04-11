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

import org.junit.Assert;
import org.junit.Test;
import pascal.taie.Main;
import pascal.taie.World;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.Subsignature;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;

import java.util.Comparator;

public class SootFrontendTest {
    
    @Test
    public void testWorldBuilder() {
        Main.buildWorld( "-pp", "-cp", "test-resources/cspta", "-m", "Assign");
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
