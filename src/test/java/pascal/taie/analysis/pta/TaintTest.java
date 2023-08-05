/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2022 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2022 Yue Li <yueli@nju.edu.cn>
 *
 * This file is part of Tai-e.
 *
 * Tai-e is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Tai-e is distributed in the hope that it will be useful,but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Tai-e. If not, see <https://www.gnu.org/licenses/>.
 */

package pascal.taie.analysis.pta;

import org.junit.jupiter.api.Test;
import pascal.taie.analysis.Tests;

public class TaintTest {

    static final String DIR = "taint";

    @Test
    void testArrayTaint() {
        Tests.testPTA(DIR, "ArrayTaint",
                "taint-config:src/test/resources/pta/taint/taint-config.yml");
    }

    @Test
    void testCharArray() {
        Tests.testPTA(DIR, "CharArray",
                "taint-config:src/test/resources/pta/taint/taint-config.yml");
    }

    @Test
    void testFieldTaint() {
        Tests.testPTA(DIR, "FieldTaint",
                "taint-config:src/test/resources/pta/taint/taint-config.yml");
    }

    @Test
    void testLinkedQueue() {
        Tests.testPTA(DIR, "LinkedQueue",
                "taint-config:src/test/resources/pta/taint/taint-config.yml");
    }

    @Test
    void testCSTaint() {
        Tests.testPTA(DIR, "CSTaint",
                "cs:1-obj;taint-config:src/test/resources/pta/taint/taint-config.yml");
    }

    @Test
    void testTwoObjectTaint() {
        Tests.testPTA(DIR, "TwoObjectTaint",
                "cs:2-obj",
                "taint-config:src/test/resources/pta/taint/taint-config.yml");
    }

    @Test
    void testStaticTaintTransfer() {
        Tests.testPTA(DIR, "StaticTaintTransfer",
                "taint-config:src/test/resources/pta/taint/taint-config-static-taint-transfer.yml");
    }

    @Test
    void testInstanceSourceSink() {
        Tests.testPTA(DIR, "InstanceSourceSink",
                "taint-config:src/test/resources/pta/taint/taint-config-instance-source-sink.yml");
    }

    @Test
    void testTaintCorner() {
        Tests.testPTA(DIR, "TaintCorner",
                "taint-config:src/test/resources/pta/taint/taint-config.yml");
    }

    @Test
    void testCycleTaint() {
        Tests.testPTA(DIR, "CycleTaint",
                "taint-config:src/test/resources/pta/taint/taint-config.yml");
    }

    @Test
    void testComplexTaint() {
        Tests.testPTA(DIR, "ComplexTaint",
                "taint-config:src/test/resources/pta/taint/taint-config.yml");
    }

    @Test
    void testTaintTransferEdge() {
        Tests.testPTA(DIR, "TaintTransferEdge",
                "taint-config:src/test/resources/pta/taint/taint-config-taint-transfer-edge.yml");
    }

    @Test
    void testSimpleTaint() {
        Tests.testPTA(DIR, "SimpleTaint",
                "taint-config:src/test/resources/pta/taint/taint-config.yml");
    }

    @Test
    void testArgToResult() {
        Tests.testPTA(DIR, "ArgToResult",
                "taint-config:src/test/resources/pta/taint/taint-config.yml");
    }

    @Test
    void testBaseToResult() {
        Tests.testPTA(DIR, "BaseToResult",
                "taint-config:src/test/resources/pta/taint/taint-config.yml");
    }

    @Test
    void testStringAppend() {
        Tests.testPTA(DIR, "StringAppend",
                "taint-config:src/test/resources/pta/taint/taint-config.yml");
    }

    @Test
    void testOneCallTaint() {
        Tests.testPTA(DIR, "OneCallTaint",
                "cs:1-call",
                "taint-config:src/test/resources/pta/taint/taint-config.yml");
    }

    @Test
    void testInterTaintTransfer() {
        Tests.testPTA(DIR, "InterTaintTransfer",
                "cs:2-call",
                "taint-config:src/test/resources/pta/taint/taint-config.yml");
    }

    @Test
    void testTaintInList() {
        Tests.testPTA(DIR, "TaintInList",
                "cs:2-obj",
                "taint-config:src/test/resources/pta/taint/taint-config.yml");
    }

    @Test
    void testBackPropagation() {
        Tests.testPTA(DIR, "BackPropagation",
                "taint-config:src/test/resources/pta/taint/taint-config.yml");
    }

    @Test
    void testCSBackPropagation() {
        Tests.testPTA(DIR, "CSBackPropagation",
                "cs:1-obj",
                "taint-config:src/test/resources/pta/taint/taint-config.yml");
    }

    @Test
    void testTaintParam() {
        Tests.testPTA(DIR, "TaintParam",
                "taint-config:src/test/resources/pta/taint/taint-config-param-source.yml");
    }

    @Test
    void testCallSiteMode() {
        Tests.testPTA(DIR, "CallSiteMode",
                "taint-config:src/test/resources/pta/taint/taint-config-call-site-model.yml");
    }
}
