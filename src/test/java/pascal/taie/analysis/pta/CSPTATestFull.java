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
        Tests.testCSPTA(DIR, "New");
    }

    @Test
    public void testAssign() {
        Tests.testCSPTA(DIR, "Assign");
    }

    @Test
    public void testStoreLoad() {
        Tests.testCSPTA(DIR, "StoreLoad");
    }

    @Test
    public void testCall() {
        Tests.testCSPTA(DIR, "Call");
    }

    @Test
    public void testAssign2() {
        Tests.testCSPTA(DIR, "Assign2");
    }

    @Test
    public void testInstanceField() {
        Tests.testCSPTA(DIR, "InstanceField");
    }

    @Test
    public void testInstanceField2() {
        Tests.testCSPTA(DIR, "InstanceField2");
    }

    @Test
    public void testCallParamRet() {
        Tests.testCSPTA(DIR, "CallParamRet");
    }

    @Test
    public void testCallField() {
        Tests.testCSPTA(DIR, "CallField");
    }

    // Tests for context sensitivity variants
    @Test
    public void testOneCall() {
        Tests.testCSPTA(DIR, "OneCall", "cs:1-call");
    }

    @Test
    public void testOneObject() {
        Tests.testCSPTA(DIR, "OneObject", "cs:1-obj");
    }

    @Test
    public void testOneType() {
        Tests.testCSPTA(DIR, "OneType", "cs:1-type");
    }

    @Test
    public void testTwoCall() {
        Tests.testCSPTA(DIR, "TwoCall", "cs:2-call");
    }

    @Test
    public void testTwoObject() {
        Tests.testCSPTA(DIR, "TwoObject", "cs:2-obj");
    }

    @Test
    public void testTwoType() {
        Tests.testCSPTA(DIR, "TwoType", "cs:2-type");
    }

    // Tests for Java feature supporting
    @Test
    public void testStaticField() {
        Tests.testCSPTA(DIR, "StaticField");
    }

    @Test
    public void testArray() {
        Tests.testCSPTA(DIR, "Array");
    }

    @Test
    public void testNull() {
        Tests.testCSPTA(DIR, "Null");
    }

    @Test
    public void testPrimitive() {
        Tests.testCSPTA(DIR, "Primitive");
    }

    // New tests
    @Test
    public void testRecursiveObj() {
        Tests.testCSPTA(DIR, "RecursiveObj", "cs:2-obj");
    }

    @Test
    public void testLongObjContext() {
        Tests.testCSPTA(DIR, "LongObjContext", "cs:2-obj");
    }

    @Test
    public void testLongCallContext() {
        Tests.testCSPTA(DIR, "LongCallContext", "cs:2-call");
    }

    @Test
    public void testStaticSelect() {
        Tests.testCSPTA(DIR, "StaticSelect", "cs:2-obj");
    }


    @Test
    public void testTwoCallOnly() {
        Tests.testCSPTA(DIR, "TwoCallOnly", "cs:2-call");
    }

    @Test
    public void testObjOnly() {
        Tests.testCSPTA(DIR, "ObjOnly", "cs:1-obj");
    }

    @Test
    public void testMustUseHeap() {
        Tests.testCSPTA(DIR, "MustUseHeap", "cs:2-call");
    }

    @Test
    public void testNestedHeap() {
        Tests.testCSPTA(DIR, "NestedHeap", "cs:2-obj");
    }

    @Test
    public void testCallOnly() {
        Tests.testCSPTA(DIR, "CallOnly", "cs:1-call");
    }

    @Test
    public void testCycle() {
        Tests.testCSPTA(DIR, "Cycle");
    }

    @Test
    public void testMultiReturn() {
        Tests.testCSPTA(DIR, "MultiReturn");
    }

    @Test
    public void testLinkedQueue() {
        Tests.testCSPTA(DIR, "LinkedQueue", "cs:2-obj");
    }
}
