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
import pascal.taie.analysis.TestUtils;

public class CSPTATest {
    
    private static final String DIR = "cspta";

    // Tests for context insensitivity
    @Test
    public void testNew() {
        TestUtils.testPTA(DIR, "New");
    }

    @Test
    public void testAssign() {
        TestUtils.testPTA(DIR, "Assign");
    }

    @Test
    public void testStoreLoad() {
        TestUtils.testPTA(DIR, "StoreLoad");
    }

    @Test
    public void testCall() {
        TestUtils.testPTA(DIR, "Call");
    }

    @Test
    public void testAssign2() {
        TestUtils.testPTA(DIR, "Assign2");
    }

    @Test
    public void testInstanceField() {
        TestUtils.testPTA(DIR, "InstanceField");
    }

    @Test
    public void testInstanceField2() {
        TestUtils.testPTA(DIR, "InstanceField2");
    }

    @Test
    public void testCallParamRet() {
        TestUtils.testPTA(DIR, "CallParamRet");
    }

    @Test
    public void testCallField() {
        TestUtils.testPTA(DIR, "CallField");
    }
    
    // Tests for context sensitivity variants
    @Test
    public void testOneCall() {
        TestUtils.testPTA(DIR, "OneCall", "cs:1-call");
    }

    @Test
    public void testOneObject() {
        TestUtils.testPTA(DIR, "OneObject", "cs:1-obj");
    }

    @Test
    public void testOneType() {
        TestUtils.testPTA(DIR, "OneType", "cs:1-type");
    }

    @Test
    public void testTwoCall() {
        TestUtils.testPTA(DIR, "TwoCall", "cs:2-call");
    }

    @Test
    public void testTwoObject() {
        TestUtils.testPTA(DIR, "TwoObject", "cs:2-obj");
    }

    @Test
    public void testTwoType() {
        TestUtils.testPTA(DIR, "TwoType", "cs:2-type");
    }

    // Tests for Java feature supporting
    @Test
    public void testStaticField() {
        TestUtils.testPTA(DIR, "StaticField");
    }

    @Test
    public void testArray() {
        TestUtils.testPTA(DIR, "Array");
    }

    @Test
    public void testCast() {
        TestUtils.testPTA(DIR, "Cast");
    }

    @Test
    public void testNull() {
        TestUtils.testPTA(DIR, "Null");
    }

    @Test
    public void testPrimitive() {
        TestUtils.testPTA(DIR, "Primitive");
    }

    @Test
    public void testStrings() {
        TestUtils.testPTA(DIR, "Strings");
    }

    @Test
    public void testMultiArray() {
        TestUtils.testPTA(DIR, "MultiArray");
    }

    @Test
    public void testClinit() {
        TestUtils.testPTA(DIR, "Clinit");
    }

    @Test
    public void testClassObj() {
        TestUtils.testPTA(DIR, "ClassObj");
    }

    // Tests for handling of non-normal objects
    @Test
    public void testTypeSens() {
        TestUtils.testPTA(DIR, "TypeSens", "cs:2-type");
    }

    @Test
    public void testSpecialHeapContext() {
        TestUtils.testPTA(DIR, "SpecialHeapContext", "cs:2-object");
    }

    @Test
    public void testNativeModel() {
        TestUtils.testPTA(DIR, "NativeModel", "only-app:false");
    }
}
