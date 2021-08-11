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

public class LambdaTest {

    private static final String DIR = "lambda";

    @Test
    public void testArgs() {
        TestUtils.testPTA(DIR, "Args");
    }

    @Test
    public void testConstructor() {
        TestUtils.testPTA(DIR, "LambdaConstructor");
    }

    @Test
    public void testInstanceMethod() {
        TestUtils.testPTA(DIR, "LambdaInstanceMethod");
    }

    @Test
    public void testStaticMethod() {
        TestUtils.testPTA(DIR, "LambdaStaticMethod");
    }

    @Test
    public void testImpreciseLambdas() {
        TestUtils.testPTA(DIR, "ImpreciseLambdas");
    }
}
