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
import pascal.taie.java.classes.JClass;

public class HierarchyTest {

    private static ClassHierarchy hierarchy;

    @BeforeClass
    public static void initTypeManager() {
        TestUtils.buildWorld(new String[]{
                "-cp",
                "java-benchmarks/jre1.6.0_24/rt.jar;" +
                        "java-benchmarks/jre1.6.0_24/jce.jar;" +
                        "java-benchmarks/jre1.6.0_24/jsse.jar;" +
                        "analyzed/java",
                "Subclasses"
        });
        hierarchy = World.get().getClassHierarchy();
    }

    /**
     * Test interface and subinterfaces.
     */
    @Test
    public void testCanAssign1() {
        JClass I = hierarchy.getClass("I");
        JClass II = hierarchy.getClass("II");
        JClass III = hierarchy.getClass("III");
        JClass IIII = hierarchy.getClass("IIII");
        Assert.assertFalse(hierarchy.canAssign(I, II));
        Assert.assertFalse(hierarchy.canAssign(II, I));
        Assert.assertFalse(hierarchy.canAssign(III, I));
        Assert.assertTrue(hierarchy.canAssign(I, III));
        Assert.assertTrue(hierarchy.canAssign(I, IIII));
    }

    /**
     * Test interfaces and java.lang.Object.
     */
    @Test
    public void testCanAssign2() {
        JClass Object = hierarchy.getJREClass("java.lang.Object");
        JClass I = hierarchy.getClass("I");
        JClass C = hierarchy.getClass("C");
        Assert.assertTrue(hierarchy.canAssign(Object, I));
        Assert.assertFalse(hierarchy.canAssign(I, Object));
        Assert.assertTrue(hierarchy.canAssign(Object, C));
        Assert.assertFalse(hierarchy.canAssign(C, Object));
    }

    /**
     * Test interface and implementers
     */
    @Test
    public void testCanAssign3() {
        JClass I = hierarchy.getClass("I");
        JClass E = hierarchy.getClass("E");
        JClass F = hierarchy.getClass("F");
        JClass G = hierarchy.getClass("G");
        Assert.assertTrue(hierarchy.canAssign(I, E));
        Assert.assertTrue(hierarchy.canAssign(I, F));
        Assert.assertFalse(hierarchy.canAssign(E, I));
        Assert.assertTrue(hierarchy.canAssign(I, G));
    }

    /**
     * Test class and subclasses.
     */
    @Test
    public void testCanAssign4() {
        JClass C = hierarchy.getClass("C");
        JClass D = hierarchy.getClass("D");
        JClass G = hierarchy.getClass("G");
        Assert.assertTrue(hierarchy.canAssign(C, D));
        Assert.assertFalse(hierarchy.canAssign(D, C));
        Assert.assertTrue(hierarchy.canAssign(C, G));
    }
}
