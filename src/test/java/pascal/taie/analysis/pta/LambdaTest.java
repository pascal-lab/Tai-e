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

public class LambdaTest {

    private static final String DIR = "lambda";

    private static final String ARG = "handle-invokedynamic:true";

    @Test
    public void testArgs() {
        Tests.testPTA(DIR, "Args", ARG);
    }

    @Test
    public void testConstructor() {
        Tests.testPTA(DIR, "LambdaConstructor", ARG);
    }

    @Test
    public void testInstanceMethod() {
        Tests.testPTA(DIR, "LambdaInstanceMethod", ARG);
    }

    @Test
    public void testStaticMethod() {
        Tests.testPTA(DIR, "LambdaStaticMethod", ARG);
    }

    @Test
    public void testImpreciseLambdas() {
        Tests.testPTA(DIR, "ImpreciseLambdas", ARG);
    }
}
