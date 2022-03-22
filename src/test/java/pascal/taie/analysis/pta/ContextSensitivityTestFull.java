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

public class ContextSensitivityTestFull extends ContextSensitivityTest {

    // More complex tests
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
    public void testLinkedQueue() {
        Tests.testPTA(DIR, "LinkedQueue", "cs:2-obj");
    }

    // Tests for handling of non-normal objects
    @Test
    public void testTypeSens() {
        Tests.testPTA(DIR, "TypeSens", "cs:2-type");
    }

    @Test
    public void testSpecialHeapContext() {
        Tests.testPTA(DIR, "SpecialHeapContext", "cs:2-obj");
    }
}
