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

import static pascal.taie.analysis.pta.CSPTATest.DIR;

public class CSPTATestFull {

    // Tests for context insensitivity
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

    // Tests for context sensitivity variants
    @Test
    public void testOneCall() {
        Tests.testPTA(DIR, "OneCall", "cs:1-call");
    }

    @Test
    public void testOneObject() {
        Tests.testPTA(DIR, "OneObject", "cs:1-obj");
    }

    @Test
    public void testOneType() {
        Tests.testPTA(DIR, "OneType", "cs:1-type");
    }

    @Test
    public void testTwoCall() {
        Tests.testPTA(DIR, "TwoCall", "cs:2-call");
    }

    @Test
    public void testTwoObject() {
        Tests.testPTA(DIR, "TwoObject", "cs:2-obj");
    }

    @Test
    public void testTwoType() {
        Tests.testPTA(DIR, "TwoType", "cs:2-type");
    }

    // Tests for Java feature supporting
    @Test
    public void testStaticField() {
        Tests.testPTA(DIR, "StaticField");
    }

    @Test
    public void testArray() {
        Tests.testPTA(DIR, "Array");
    }

    @Test
    public void testNull() {
        Tests.testPTA(DIR, "Null");
    }

    @Test
    public void testPrimitive() {
        Tests.testPTA(DIR, "Primitive");
    }

    // New tests
    @Test
    public void testRecursiveObj() {
        Tests.testPTA(DIR, "RecursiveObj", "cs:2-obj");
    }

    @Test
    public void testLongObjContext() {
        Tests.testPTA(DIR, "LongObjContext", "cs:2-obj");
    }

    @Test
    public void testLongCallContext() {
        Tests.testPTA(DIR, "LongCallContext", "cs:2-call");
    }

    @Test
    public void testStaticSelect() {
        Tests.testPTA(DIR, "StaticSelect", "cs:2-obj");
    }


    @Test
    public void testTwoCallOnly() {
        Tests.testPTA(DIR, "TwoCallOnly", "cs:2-call");
    }

    @Test
    public void testObjOnly() {
        Tests.testPTA(DIR, "ObjOnly", "cs:1-obj");
    }

    @Test
    public void testMustUseHeap() {
        Tests.testPTA(DIR, "MustUseHeap", "cs:2-call");
    }

    @Test
    public void testNestedHeap() {
        Tests.testPTA(DIR, "NestedHeap", "cs:2-obj");
    }

    @Test
    public void testCallOnly() {
        Tests.testPTA(DIR, "CallOnly", "cs:1-call");
    }

    @Test
    public void testCycle() {
        Tests.testPTA(DIR, "Cycle");
    }

    @Test
    public void testMultiReturn() {
        Tests.testPTA(DIR, "MultiReturn");
    }

    @Test
    public void testLinkedQueue() {
        Tests.testPTA(DIR, "LinkedQueue", "cs:2-obj");
    }
}
