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

import static pascal.taie.analysis.pta.CIPTATest.DIR;

public class CIPTATestFull {

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
    public void testStoreLoad() {
        Tests.testCIPTA(DIR, "StoreLoad");
    }

    @Test
    public void testCall() {
        Tests.testCIPTA(DIR, "Call");
    }

    @Test
    public void testAssign2() {
        Tests.testCIPTA(DIR, "Assign2");
    }

    @Test
    public void testInstanceField() {
        Tests.testCIPTA(DIR, "InstanceField");
    }

    @Test
    public void testInstanceField2() {
        Tests.testCIPTA(DIR, "InstanceField2");
    }

    @Test
    public void testCallParamRet() {
        Tests.testCIPTA(DIR, "CallParamRet");
    }

    @Test
    public void testCallField() {
        Tests.testCIPTA(DIR, "CallField");
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
    public void testLinkedQueue() {
        Tests.testCIPTA(DIR, "LinkedQueue");
    }

    @Test
    public void testRedBlackBST() {
        Tests.testCIPTA(DIR, "RedBlackBST");
    }

    @Test
    public void testMultiReturn() {
        Tests.testCIPTA(DIR, "MultiReturn");
    }

    @Test
    public void testDispatch() {
        Tests.testCIPTA(DIR, "Dispatch");
    }

    @Test
    public void testInterface() {
        Tests.testCIPTA(DIR, "Interface");
    }

    @Test
    public void testRecursion() {
        Tests.testCIPTA(DIR, "Recursion");
    }

    @Test
    public void testCycle() {
        Tests.testCIPTA(DIR, "Cycle");
    }

    @Test
    public void testComplexAssign() {
        Tests.testCIPTA(DIR, "ComplexAssign");
    }

    @Test
    public void testMergeParam() {
        Tests.testCIPTA(DIR, "MergeParam");
    }
}
