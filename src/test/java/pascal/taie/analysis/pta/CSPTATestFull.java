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

public class CSPTATestFull extends CSPTATest {

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
