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
    public void testArrayTaint() {
        Tests.testPTA(DIR, "ArrayTaint",
                "taint-config:src/test/resources/pta/taint/taint-config.yml");
    }

    @Test
    public void testCharArray() {
        Tests.testPTA(DIR, "CharArray",
                "taint-config:src/test/resources/pta/taint/taint-config.yml");
    }

    @Test
    public void testFieldTaint() {
        Tests.testPTA(DIR, "FieldTaint",
                "taint-config:src/test/resources/pta/taint/taint-config.yml");
    }

    @Test
    public void testLinkedQueue() {
        Tests.testPTA(DIR, "LinkedQueue",
                "taint-config:src/test/resources/pta/taint/taint-config.yml");
    }

    @Test
    public void testCSTaint() {
        Tests.testPTA(DIR, "CSTaint",
                "cs:1-obj;taint-config:src/test/resources/pta/taint/taint-config.yml");
    }

    @Test
    public void testTwoObjectTaint() {
        Tests.testPTA(DIR, "TwoObjectTaint",
                "cs:2-obj;taint-config:src/test/resources/pta/taint/taint-config.yml");
    }

    @Test
    public void testStaticTaintTransfer() {
        Tests.testPTA(DIR, "StaticTaintTransfer",
                "taint-config:src/test/resources/pta/taint/taint-config-static-taint-transfer.yml");
    }

    @Test
    public void testInstanceSourceSink() {
        Tests.testPTA(DIR, "InstanceSourceSink",
                "taint-config:src/test/resources/pta/taint/taint-config-instance-source-sink.yml");
    }

    @Test
    public void testTaintCorner() {
        Tests.testPTA(DIR, "TaintCorner",
                "taint-config:src/test/resources/pta/taint/taint-config.yml");
    }

    @Test
    public void testCycleTaint() {
        Tests.testPTA(DIR, "CycleTaint",
                "taint-config:src/test/resources/pta/taint/taint-config.yml");
    }


    @Test
    public void testComplexTaint() {
        Tests.testPTA(DIR, "ComplexTaint",
                "taint-config:src/test/resources/pta/taint/taint-config.yml");
    }

    @Test
    public void testTaintTransferEdge() {
        Tests.testPTA(DIR, "TaintTransferEdge",
                "taint-config:src/test/resources/pta/taint/taint-config-taint-transfer-edge.yml");
    }

    @Test
    public void testSimpleTaint() {
        Tests.testPTA(DIR, "SimpleTaint",
                "taint-config:src/test/resources/pta/taint/taint-config.yml");
    }

    @Test
    public void testArgToResult() {
        Tests.testPTA(DIR, "ArgToResult",
                "taint-config:src/test/resources/pta/taint/taint-config.yml");
    }

    @Test
    public void testBaseToResult() {
        Tests.testPTA(DIR, "BaseToResult",
                "taint-config:src/test/resources/pta/taint/taint-config.yml");
    }

    @Test
    public void testStringAppend() {
        Tests.testPTA(DIR, "StringAppend",
                "taint-config:src/test/resources/pta/taint/taint-config.yml");
    }

    @Test
    public void testOneCallTaint() {
        Tests.testPTA(DIR, "OneCallTaint",
                "cs:1-call;taint-config:src/test/resources/pta/taint/taint-config.yml");
    }

    @Test
    public void testInterTaintTransfer() {
        Tests.testPTA(DIR, "InterTaintTransfer",
                "cs:2-call;taint-config:src/test/resources/pta/taint/taint-config.yml");
    }

    @Test
    public void testTaintInList() {
        Tests.testPTA(DIR, "TaintInList",
                "cs:2-obj;taint-config:src/test/resources/pta/taint/taint-config.yml");
    }
}
