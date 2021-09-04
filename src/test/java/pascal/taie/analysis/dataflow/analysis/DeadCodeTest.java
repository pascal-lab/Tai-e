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

package pascal.taie.analysis.dataflow.analysis;

import org.junit.Test;
import pascal.taie.analysis.Tests;
import pascal.taie.analysis.dataflow.analysis.DeadCodeDetection;

public class DeadCodeTest {

    static void testDCD(String inputClass) {
        Tests.testDFA(inputClass, "src/test/resources/dataflow/deadcode/",
                DeadCodeDetection.ID);
    }

    @Test
    public void testControlFlowUnreachable() {
        testDCD("ControlFlowUnreachable");
    }

    @Test
    public void testUnreachableBranch() {
        testDCD("UnreachableBranch");
    }

    @Test
    public void testDeadAssignment() {
        testDCD("DeadAssignment");
    }
}
