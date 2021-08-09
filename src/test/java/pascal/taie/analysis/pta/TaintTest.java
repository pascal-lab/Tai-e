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
                "taint-config:src/test/resources/pta/taint/taint-config.yml");
    }

    @Test
    public void testFieldTaint() {
        TestUtils.testPTA(DIR, "FieldTaint",
                "taint-config:src/test/resources/pta/taint/taint-config.yml");
    }

    @Test
    public void testCSTaint() {
        TestUtils.testPTA(DIR, "CSTaint",
                "cs:1-obj;taint-config:src/test/resources/pta/taint/taint-config.yml");
    }

    @Test
    public void testArgToBase() {
        TestUtils.testPTA(DIR, "ArgToBase",
                "taint-config:src/test/resources/pta/taint/taint-config.yml");
    }

    @Test
    public void testArgToResult() {
        TestUtils.testPTA(DIR, "ArgToResult",
                "taint-config:src/test/resources/pta/taint/taint-config.yml");
    }

    @Test
    public void testBaseToResult() {
        TestUtils.testPTA(DIR, "BaseToResult",
                "taint-config:src/test/resources/pta/taint/taint-config.yml");
    }
}
