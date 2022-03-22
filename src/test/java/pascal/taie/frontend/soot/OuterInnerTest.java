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
import org.junit.BeforeClass;
import org.junit.Test;
import pascal.taie.Main;
import pascal.taie.World;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;

@SuppressWarnings("ConstantConditions")
public class OuterInnerTest {

    private static ClassHierarchy hierarchy;

    @BeforeClass
    public static void beforeClass() {
        Main.buildWorld("-pp", "-cp", "src/test/resources/world",
                "-m", "OuterInner");
        hierarchy = World.get().getClassHierarchy();
    }

    @Test
    public void testOuter() {
        JClass main = hierarchy.getClass("OuterInner");
        Assert.assertFalse(main.hasOuterClass());
        Assert.assertEquals(2, hierarchy.getDirectInnerClassesOf(main).size());

        JClass Inner = hierarchy.getClass("OuterInner$Inner");
        Assert.assertTrue(Inner.hasOuterClass());

        JClass Outer = hierarchy.getClass("OuterInner$Outer");
        Assert.assertEquals(3, hierarchy.getDirectInnerClassesOf(Outer).size());

        JClass OuterInner1 = hierarchy.getClass("OuterInner$Outer$Inner1");
        Assert.assertEquals(0, hierarchy.getDirectInnerClassesOf(OuterInner1).size());
    }
}
