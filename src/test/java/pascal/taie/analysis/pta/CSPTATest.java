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

public class CSPTATest {

    static final String DIR = "cspta";

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
    public void testInstanceField() {
        Tests.testCSPTA(DIR, "InstanceField");
    }

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

    @Test
    public void testStaticField() {
        Tests.testCSPTA(DIR, "StaticField");
    }

    @Test
    public void testArray() {
        Tests.testCSPTA(DIR, "Array");
    }
}
