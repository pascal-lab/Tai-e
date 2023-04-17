/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2022 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2022 Yue Li <yueli@nju.edu.cn>
 *
 * This file is part of Tai-e.
 *
 * Tai-e is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Tai-e is distributed in the hope that it will be useful,but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Tai-e. If not, see <https://www.gnu.org/licenses/>.
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
                "--input-classes", "OuterInner");
        hierarchy = World.get().getClassHierarchy();
    }

    @Test
    public void testOuter() {
        JClass main = hierarchy.getClass("OuterInner");
        Assert.assertFalse(main.hasOuterClass());
        Assert.assertEquals(2, hierarchy.getDirectInnerClassesOf(main).size());

        JClass inner = hierarchy.getClass("OuterInner$Inner");
        Assert.assertTrue(inner.hasOuterClass());

        JClass outer = hierarchy.getClass("OuterInner$Outer");
        Assert.assertEquals(3, hierarchy.getDirectInnerClassesOf(outer).size());

        JClass outerInner1 = hierarchy.getClass("OuterInner$Outer$Inner1");
        Assert.assertEquals(0, hierarchy.getDirectInnerClassesOf(outerInner1).size());
    }
}
