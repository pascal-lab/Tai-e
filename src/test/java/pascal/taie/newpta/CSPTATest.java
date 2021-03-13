/*
 * Tai-e: A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Tai-e is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.taie.newpta;

import org.junit.Test;
import pascal.taie.TestUtils;

public class CSPTATest {

    // Tests for context sensitivity variants
    @Test
    public void testOneCall() {
        TestUtils.testNewCSPTA("OneCall", "-cs", "1-call");
    }

    @Test
    public void testOneObject() {
        TestUtils.testNewCSPTA("OneObject", "-cs", "1-obj");
    }

    @Test
    public void testOneType() {
        TestUtils.testNewCSPTA("OneType", "-cs", "1-type");
    }

    @Test
    public void testTwoCall() {
        TestUtils.testNewCSPTA("TwoCall", "-cs", "2-call");
    }

    @Test
    public void testTwoObject() {
        TestUtils.testNewCSPTA("TwoObject", "-cs", "2-obj");
    }

    @Test
    public void testTwoType() {
        TestUtils.testNewCSPTA("TwoType", "-cs", "2-type");
    }

    // Tests for Java feature supporting
    @Test
    public void testStaticField() {
        TestUtils.testNewCSPTA("StaticField");
    }

    @Test
    public void testArray() {
        TestUtils.testNewCSPTA("Array");
    }

    @Test
    public void testCast() {
        TestUtils.testNewCSPTA("Cast");
    }

    @Test
    public void testNull() {
        TestUtils.testNewCSPTA("Null");
    }

    @Test
    public void testPrimitive() {
        TestUtils.testNewCSPTA("Primitive");
    }

    @Test
    public void testStrings() {
        TestUtils.testNewCSPTA("Strings");
    }

    @Test
    public void testMultiArray() {
        TestUtils.testNewCSPTA("MultiArray");
    }

    @Test
    public void testClinit() {
        TestUtils.testNewCSPTA("Clinit");
    }

    @Test
    public void testClassObj() {
        TestUtils.testNewCSPTA("ClassObj");
    }

    // Tests for handling of non-normal objects
    @Test
    public void testTypeSens() {
        TestUtils.testNewCSPTA("TypeSens", "-cs", "2-type");
    }

    @Test
    public void testSpecialHeapContext() {
        TestUtils.testNewCSPTA("SpecialHeapContext", "-cs", "2-object");
    }

    @Test
    public void testNativeModel() {
        TestUtils.testNewCSPTA("NativeModel");
    }
}
