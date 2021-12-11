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

public class TaintTestFull extends TaintTest {

    @Test
    public void testArrayTaint() {
        Tests.testCSPTA(DIR, "ArrayTaint",
                "taint-config:src/test/resources/pta/taint/taint-config.yml");
    }

    @Test
    public void testCharArray() {
        Tests.testCSPTA(DIR, "CharArray",
                "taint-config:src/test/resources/pta/taint/taint-config.yml");
    }

    @Test
    public void testFieldTaint() {
        Tests.testCSPTA(DIR, "FieldTaint",
                "taint-config:src/test/resources/pta/taint/taint-config.yml");
    }

    @Test
    public void testLinkedQueue() {
        Tests.testCSPTA(DIR, "LinkedQueue",
                "taint-config:src/test/resources/pta/taint/taint-config.yml");
    }

    @Test
    public void testCSTaint() {
        Tests.testCSPTA(DIR, "CSTaint",
                "cs:1-obj;taint-config:src/test/resources/pta/taint/taint-config.yml");
    }

    @Test
    public void testTwoObjectTaint() {
        Tests.testCSPTA(DIR, "TwoObjectTaint",
                "cs:2-obj;taint-config:src/test/resources/pta/taint/taint-config.yml");
    }

    @Test
    public void testStaticTaintTransfer() {
        Tests.testCSPTA(DIR, "StaticTaintTransfer",
                "taint-config:src/test/resources/pta/taint/taint-config-static-taint-transfer.yml");
    }

    @Test
    public void testInstanceSourceSink() {
        Tests.testCSPTA(DIR, "InstanceSourceSink",
                "taint-config:src/test/resources/pta/taint/taint-config-instance-source-sink.yml");
    }

    @Test
    public void testTaintCorner() {
        Tests.testCSPTA(DIR, "TaintCorner",
                "taint-config:src/test/resources/pta/taint/taint-config.yml");
    }

    @Test
    public void testCycleTaint() {
        Tests.testCSPTA(DIR, "CycleTaint",
                "taint-config:src/test/resources/pta/taint/taint-config.yml");
    }


    @Test
    public void testComplexTaint() {
        Tests.testCSPTA(DIR, "ComplexTaint",
                "taint-config:src/test/resources/pta/taint/taint-config.yml");
    }

    @Test
    public void testTaintTransferEdge() {
        Tests.testCSPTA(DIR, "TaintTransferEdge",
                "taint-config:src/test/resources/pta/taint/taint-config-taint-transfer-edge.yml");
    }
}
