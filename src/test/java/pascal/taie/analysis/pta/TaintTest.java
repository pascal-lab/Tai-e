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

    @Test
    public void testBackPropagation() {
        Tests.testPTA(DIR, "BackPropagation",
                "taint-config:src/test/resources/pta/taint/taint-config.yml");
    }

    @Test
    public void testCSBackPropagation() {
        Tests.testPTA(DIR, "CSBackPropagation",
                "cs:1-obj;taint-config:src/test/resources/pta/taint/taint-config.yml");
    }

    @Test
    public void testTaintParam() {
        Tests.testPTA(DIR, "TaintParam",
                "taint-config:src/test/resources/pta/taint/taint-config-param-source.yml");
    }
}
