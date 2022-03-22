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

/**
 * Tests basic functionalities of pointer analysis
 */
public class BasicTest {

    static final String DIR = "basic";

    // Tests for handling basic pointer analysis statements
    @Test
    public void testNew() {
        Tests.testPTA(DIR, "New");
    }

    @Test
    public void testAssign() {
        Tests.testPTA(DIR, "Assign");
    }

    @Test
    public void testStoreLoad() {
        Tests.testPTA(DIR, "StoreLoad");
    }

    @Test
    public void testCall() {
        Tests.testPTA(DIR, "Call");
    }

    @Test
    public void testAssign2() {
        Tests.testPTA(DIR, "Assign2");
    }

    @Test
    public void testInstanceField() {
        Tests.testPTA(DIR, "InstanceField");
    }

    @Test
    public void testInstanceField2() {
        Tests.testPTA(DIR, "InstanceField2");
    }

    @Test
    public void testCallParamRet() {
        Tests.testPTA(DIR, "CallParamRet");
    }

    @Test
    public void testCallField() {
        Tests.testPTA(DIR, "CallField");
    }

    @Test
    public void testStaticCall() {
        Tests.testPTA(DIR, "StaticCall");
    }

    @Test
    public void testMergeParam() {
        Tests.testPTA(DIR, "MergeParam");
    }

    @Test
    public void testLinkedQueue() {
        Tests.testPTA(DIR, "LinkedQueue");
    }

    @Test
    public void testRedBlackBST() {
        Tests.testPTA(DIR, "RedBlackBST");
    }

    @Test
    public void testMultiReturn() {
        Tests.testPTA(DIR, "MultiReturn");
    }

    @Test
    public void testDispatch() {
        Tests.testPTA(DIR, "Dispatch");
    }

    @Test
    public void testInterface() {
        Tests.testPTA(DIR, "Interface");
    }

    @Test
    public void testRecursion() {
        Tests.testPTA(DIR, "Recursion");
    }

    @Test
    public void testCycle() {
        Tests.testPTA(DIR, "Cycle");
    }

    @Test
    public void testComplexAssign() {
        Tests.testPTA(DIR, "ComplexAssign");
    }
}
