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

package pascal.taie.language;

import org.junit.BeforeClass;
import org.junit.Test;
import pascal.taie.Main;

import static pascal.taie.language.HierarchyTest.testResolveMethod;

public class DefaultMethodTest {

    @BeforeClass
    public static void initTypeManager() {
        Main.buildWorld("-cp", "test-resources/basic", "-m", "DefaultMethod");
    }

    @Test
    public void testDefaultMethod() {
        testResolveMethod("DefaultMethod$C", "foo", "DefaultMethod$A");
        testResolveMethod("DefaultMethod$C", "bar", "DefaultMethod$II");
    }
}
