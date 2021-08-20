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

public class ExceptionTest {

    private static final String DIR = "exception";

    @Test
    public void testExceptionCircle() {
        Tests.testPTA(DIR, "ExceptionCircle");
    }

    @Test
    public void testExceptionCircleAndRecursion() {
        Tests.testPTA(DIR, "ExceptionCircleAndRecursion");
    }

    @Test
    public void testExceptionNoneCaught() {
        Tests.testPTA(DIR, "ExceptionNoneCaught");
    }

    @Test
    public void testExceptionTreeAndRecursion() {
        Tests.testPTA(DIR, "ExceptionTreeAndRecursion");
    }
}
