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

package pascal.taie.analysis.oldpta;

import org.junit.Test;
import pascal.taie.TestUtils;

public class CSPTATest {

    // Tests for context sensitivity variants
    @Test
    public void testOneCall() {
        TestUtils.testOldCSPTA("OneCall", "-cs", "1-call");
    }

    @Test
    public void testOneObject() {
        TestUtils.testOldCSPTA("OneObject", "-cs", "1-obj");
    }

    @Test
    public void testOneType() {
        TestUtils.testOldCSPTA("OneType", "-cs", "1-type");
    }

    @Test
    public void testTwoCall() {
        TestUtils.testOldCSPTA("TwoCall", "-cs", "2-call");
    }

    @Test
    public void testTwoObject() {
        TestUtils.testOldCSPTA("TwoObject", "-cs", "2-obj");
    }

    @Test
    public void testTwoType() {
        TestUtils.testOldCSPTA("TwoType", "-cs", "2-type");
    }

    // Tests for Java feature supporting
    @Test
    public void testStaticField() {
        TestUtils.testOldCSPTA("StaticField");
    }

    @Test
    public void testArray() {
        TestUtils.testOldCSPTA("Array");
    }

    @Test
    public void testCast() {
        TestUtils.testOldCSPTA("Cast");
    }

    @Test
    public void testNull() {
        TestUtils.testOldCSPTA("Null");
    }

    @Test
    public void testPrimitive() {
        TestUtils.testOldCSPTA("Primitive");
    }

    @Test
    public void testStrings() {
        TestUtils.testOldCSPTA("Strings");
    }

    @Test
    public void testMultiArray() {
        TestUtils.testOldCSPTA("MultiArray");
    }

    @Test
    public void testClinit() {
        TestUtils.testOldCSPTA("Clinit");
    }

    @Test
    public void testClassObj() {
        TestUtils.testOldCSPTA("ClassObj");
    }

    // Tests for handling of non-normal objects
    @Test
    public void testTypeSens() {
        TestUtils.testOldCSPTA("TypeSens", "-cs", "2-type");
    }

    @Test
    public void testSpecialHeapContext() {
        TestUtils.testOldCSPTA("SpecialHeapContext", "-cs", "2-object");
    }

    @Test
    public void testNativeModel() {
        TestUtils.testOldCSPTA("NativeModel");
    }
}
