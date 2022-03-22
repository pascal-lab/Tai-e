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

import org.junit.Ignore;
import org.junit.Test;
import pascal.taie.analysis.Tests;

public class BasicTestFull extends BasicTest {

    // Tests for handling of more Java features
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
    public void testCast2() {
        Tests.testPTA(DIR, "Cast2");
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

    @Test
    public void testNative() {
        Tests.testPTA(DIR, "Native");
    }

    @Test
    @Ignore // FIXME: this test cases take long time after updating to Java 17
    public void testNativeModel() {
        Tests.testPTA(DIR, "NativeModel", "only-app:false");
    }
}
