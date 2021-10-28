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

public class PTATest {

    private static final String DIR = "cspta";

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
    public void testCast() {
        Tests.testPTA(DIR, "Cast");
    }

    @Test
    public void testNull() {
        Tests.testPTA(DIR, "Null");
    }

    @Test
    public void testPrimitive() {
        Tests.testPTA(DIR, "Primitive");
    }

    @Test
    public void testStrings() {
        Tests.testPTA(DIR, "Strings");
    }

    @Test
    public void testMultiArray() {
        Tests.testPTA(DIR, "MultiArray");
    }

    @Test
    public void testClinit() {
        Tests.testPTA(DIR, "Clinit");
    }

    @Test
    public void testClassObj() {
        Tests.testPTA(DIR, "ClassObj");
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

    @Test
    public void testNativeModel() {
        Tests.testPTA(DIR, "NativeModel", "only-app:false");
    }
}
