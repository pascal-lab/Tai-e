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

package pascal.taie.analysis.pta;

import org.junit.Test;
import pascal.taie.analysis.Tests;

public class CIPTATest {

    static final String DIR = "cipta";

    @Test
    public void testExample() {
        Tests.testCIPTA(DIR, "Example");
    }

    @Test
    public void testArray() {
        Tests.testCIPTA(DIR, "Array");
    }

    @Test
    public void testAssign() {
        Tests.testCIPTA(DIR, "Assign");
    }

    @Test
    public void testAssign2() {
        Tests.testCIPTA(DIR, "Assign2");
    }

    @Test
    public void testStoreLoad() {
        Tests.testCIPTA(DIR, "StoreLoad");
    }

    @Test
    public void testCall() {
        Tests.testCIPTA(DIR, "Call");
    }

    @Test
    public void testInstanceField() {
        Tests.testCIPTA(DIR, "InstanceField");
    }

    @Test
    public void testStaticField() {
        Tests.testCIPTA(DIR, "StaticField");
    }

    @Test
    public void testStaticCall() {
        Tests.testCIPTA(DIR, "StaticCall");
    }

    @Test
    public void testMergeParam() {
        Tests.testCIPTA(DIR, "MergeParam");
    }
}
