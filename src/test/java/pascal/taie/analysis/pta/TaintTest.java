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

public class TaintTest {

    private static final String DIR = "taint";

    @Test
    public void testSimpleTaint() {
        TestUtils.testPTA(DIR, "SimpleTaint",
                "taint:true;taint.sources-sinks:src/test/resources/pta/taint/sources-sinks.yml");
    }

    @Test
    public void testFieldTaint() {
        TestUtils.testPTA(DIR, "FieldTaint",
                "taint:true;taint.sources-sinks:src/test/resources/pta/taint/sources-sinks.yml");
    }

    @Test
    public void testCSTaint() {
        TestUtils.testPTA(DIR, "CSTaint",
                "cs:1-obj;taint:true;taint.sources-sinks:src/test/resources/pta/taint/sources-sinks.yml");
    }
}
