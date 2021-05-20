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

    // Tests for context insensitivity
    @Test
    public void testNew() {
        TestUtils.testCSPTA("New", "-pp");
    }

    @Test
    public void testAssign() {
        TestUtils.testCSPTA("Assign", "-pp");
    }

    @Test
    public void testStoreLoad() {
        TestUtils.testCSPTA("StoreLoad", "-pp");
    }

    @Test
    public void testCall() {
        TestUtils.testCSPTA("Call", "-pp");
    }

    @Test
    public void testAssign2() {
        TestUtils.testCSPTA("Assign2", "-pp");
    }

    @Test
    public void testInstanceField() {
        TestUtils.testCSPTA("InstanceField", "-pp");
    }

    @Test
    public void testInstanceField2() {
        TestUtils.testCSPTA("InstanceField2", "-pp");
    }

    @Test
    public void testCallParamRet() {
        TestUtils.testCSPTA("CallParamRet", "-pp");
    }

    @Test
    public void testCallField() {
        TestUtils.testCSPTA("CallField", "-pp");
    }
    
    // Tests for context sensitivity variants
    @Test
    public void testOneCall() {
        TestUtils.testCSPTA("OneCall", "-pp", "pta=cs:1-call");
    }

    @Test
    public void testOneObject() {
        TestUtils.testCSPTA("OneObject", "-pp", "pta=cs:1-obj");
    }

    @Test
    public void testOneType() {
        TestUtils.testCSPTA("OneType", "-pp", "pta=cs:1-type");
    }

    @Test
    public void testTwoCall() {
        TestUtils.testCSPTA("TwoCall", "-pp", "pta=cs:2-call");
    }

    @Test
    public void testTwoObject() {
        TestUtils.testCSPTA("TwoObject", "-pp", "pta=cs:2-obj");
    }

    @Test
    public void testTwoType() {
        TestUtils.testCSPTA("TwoType", "-pp", "pta=cs:2-type");
    }

    // Tests for Java feature supporting
    @Test
    public void testStaticField() {
        TestUtils.testCSPTA("StaticField", "-pp");
    }

    @Test
    public void testArray() {
        TestUtils.testCSPTA("Array", "-pp");
    }

    @Test
    public void testCast() {
        TestUtils.testCSPTA("Cast", "-pp");
    }

    @Test
    public void testNull() {
        TestUtils.testCSPTA("Null", "-pp");
    }

    @Test
    public void testPrimitive() {
        TestUtils.testCSPTA("Primitive", "-pp");
    }

    @Test
    public void testStrings() {
        TestUtils.testCSPTA("Strings");
    }

    @Test
    public void testMultiArray() {
        TestUtils.testCSPTA("MultiArray", "-pp");
    }

    @Test
    public void testClinit() {
        TestUtils.testCSPTA("Clinit");
    }

    @Test
    public void testClassObj() {
        TestUtils.testCSPTA("ClassObj");
    }

    // Tests for handling of non-normal objects
    @Test
    public void testTypeSens() {
        TestUtils.testCSPTA("TypeSens", "pta=cs:2-type");
    }

    @Test
    public void testSpecialHeapContext() {
        TestUtils.testCSPTA("SpecialHeapContext", "pta=cs:2-object");
    }

    @Test
    public void testNativeModel() {
        TestUtils.testCSPTA("NativeModel", "-pp");
    }
}
