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

    private static final String DIR = "taint";

    @Test
    public void testSimpleTaint() {
        Tests.testCSPTA(DIR, "SimpleTaint",
                "taint-config:src/test/resources/pta/taint/taint-config.yml");
    }

    @Test
    public void testFieldTaint() {
        Tests.testCSPTA(DIR, "FieldTaint",
                "taint-config:src/test/resources/pta/taint/taint-config.yml");
    }

    @Test
    public void testCSTaint() {
        Tests.testCSPTA(DIR, "CSTaint",
                "cs:1-obj;taint-config:src/test/resources/pta/taint/taint-config.yml");
    }

    @Test
    public void testArgToBase() {
        Tests.testCSPTA(DIR, "ArgToBase",
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
}
