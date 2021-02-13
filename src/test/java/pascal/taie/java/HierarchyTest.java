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

package pascal.taie.java;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import pascal.taie.frontend.soot.TestUtils;
import pascal.taie.java.classes.FieldReference;
import pascal.taie.java.classes.FieldResolutionFailedException;
import pascal.taie.java.classes.JClass;
import pascal.taie.java.classes.JField;
import pascal.taie.java.types.Type;

public class HierarchyTest {

    private static ClassHierarchy hierarchy;

    private static TypeManager typeManager;

    @BeforeClass
    public static void initTypeManager() {
        TestUtils.buildWorld(new String[]{
                "-cp",
                "java-benchmarks/jre1.6.0_24/rt.jar;" +
                        "java-benchmarks/jre1.6.0_24/jce.jar;" +
                        "java-benchmarks/jre1.6.0_24/jsse.jar;" +
                        "analyzed/java",
                "Hierarchy"
        });
        hierarchy = World.get().getClassHierarchy();
        typeManager = World.get().getTypeManager();
    }

    // ---------- Test subclass checking canAssign() ----------
    /**
     * Test interface and subinterfaces.
     */
    @Test
    public void testCanAssign1() {
        String I = "I", II = "II", III = "III", IIII = "IIII";
        expectedCanAssign(I, III);
        expectedCanAssign(I, IIII);
        expectedCannotAssign(I, II);
        expectedCannotAssign(II, I);
        expectedCannotAssign(III, I);
    }

    /**
     * Test interfaces and java.lang.Object.
     */
    @Test
    public void testCanAssign2() {
        String Object = "java.lang.Object", I = "I", C = "C";
        expectedCanAssign(Object, I);
        expectedCannotAssign(I, Object);
        expectedCanAssign(Object, C);
        expectedCannotAssign(C, Object);
    }

    /**
     * Test interface and implementers
     */
    @Test
    public void testCanAssign3() {
        String I = "I", E = "E", F = "F", G = "G";
        expectedCanAssign(I, E);
        expectedCanAssign(I, F);
        expectedCanAssign(I, G);
        expectedCannotAssign(E, I);
    }

    /**
     * Test class and subclasses.
     */
    @Test
    public void testCanAssign4() {
        String C = "C", D = "D", G = "G";
        expectedCanAssign(C, D);
        expectedCanAssign(C, G);
        expectedCannotAssign(D, C);
    }

    private static void expectedCanAssign(String toClass, String fromClass) {
        JClass to = hierarchy.getClass(toClass);
        JClass from = hierarchy.getClass(fromClass);
        Assert.assertTrue(hierarchy.canAssign(to, from));
    }

    private static void expectedCannotAssign(String toClass, String fromClass) {
        JClass to = hierarchy.getClass(toClass);
        JClass from = hierarchy.getClass(fromClass);
        Assert.assertFalse(hierarchy.canAssign(to, from));
    }

    // ---------- Test field resolution resolveField()  ----------

    /**
     * Find field in current class.
     */
    @Test
    public void testResolveField1() {
        testResolveField("E", "fe", "E");
    }

    /**
     * Find field in superclass.
     */
    @Test
    public void testResolveField2() {
        testResolveField("G", "fc", "C");
    }

    /**
     * Find field in superinterface.
     */
    @Test
    public void testResolveField3() {
        testResolveField("F", "fii", "II");
    }

    /**
     * Find field in superclass, the first matching field should be resolved.
     */
    @Test
    public void testResolveField4() {
        testResolveField("G", "f", "E");
    }

    /**
     * Find non-exist field.
     */
    @Test(expected = FieldResolutionFailedException.class)
    public void testResolveField5() {
        testResolveField("G", "xxx", "G");
    }

    /**
     * Resolve the same field twice.
     */
    @Test
    public void testResolveField6() {
        testResolveField("G", "f", "E");
        testResolveField("G", "f", "E");
    }


    /**
     * Test resolveField() with specified class and field names.
     * The declaring class of the resolved field should be the same
     * as the given expected class.
     * @param refClass declaring class of the reference
     * @param refName field name of the reference
     * @param declaringClass expected declaring class of the resolved field
     */
    private static void testResolveField(
            String refClass, String refName, String declaringClass) {
        JClass refJClass = hierarchy.getClass(refClass);
        JClass declaringJClass = hierarchy.getClass(declaringClass);
        Type refType = typeManager.getClassType("java.lang.String");
        FieldReference fieldRef = new FieldReference(refJClass, refName, refType);
        JField field = hierarchy.resolveField(fieldRef);
        Assert.assertTrue(fieldRef.isResolved());
        Assert.assertEquals(declaringJClass, field.getDeclaringClass());
    }
}
