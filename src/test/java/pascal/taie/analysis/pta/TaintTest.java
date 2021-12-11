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

public class TaintTest {

    static final String DIR = "taint";

    @Test
    public void testSimpleTaint() {
        Tests.testCSPTA(DIR, "SimpleTaint",
                "taint-config:src/test/resources/pta/taint/taint-config.yml");
    }

    @Test
    public void testArgToResult() {
        Tests.testCSPTA(DIR, "ArgToResult",
                "taint-config:src/test/resources/pta/taint/taint-config.yml");
    }

    @Test
    public void testBaseToResult() {
        Tests.testCSPTA(DIR, "BaseToResult",
                "taint-config:src/test/resources/pta/taint/taint-config.yml");
    }

    @Test
    public void testStringAppend() {
        Tests.testCSPTA(DIR, "StringAppend",
                "taint-config:src/test/resources/pta/taint/taint-config.yml");
    }

    @Test
    public void testOneCallTaint() {
        Tests.testCSPTA(DIR, "OneCallTaint",
                "cs:1-call;taint-config:src/test/resources/pta/taint/taint-config.yml");
    }

    @Test
    public void testInterTaintTransfer() {
        Tests.testCSPTA(DIR, "InterTaintTransfer",
                "cs:2-call;taint-config:src/test/resources/pta/taint/taint-config.yml");
    }

    @Test
    public void testTaintInList() {
        Tests.testCSPTA(DIR, "TaintInList",
                "cs:2-obj;taint-config:src/test/resources/pta/taint/taint-config.yml");
    }
}
