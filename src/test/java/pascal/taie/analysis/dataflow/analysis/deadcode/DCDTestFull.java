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

package pascal.taie.analysis.dataflow.analysis.deadcode;

import org.junit.Test;
import pascal.taie.analysis.TestUtils;

public class DCDTestFull {

    @Test
    public void testControlFlowUnreachable() {
        TestUtils.testDCD("ControlFlowUnreachable");
    }

    @Test
    public void testUnreachableBranch() {
        TestUtils.testDCD("UnreachableBranch");
    }

    @Test
    public void testDeadAssignment() {
        TestUtils.testDCD("DeadAssignment");
    }

    @Test
    public void testControlFlowUnreachable2() {
        TestUtils.testDCD("ControlFlowUnreachable2");
    }

    @Test
    public void testUnreachableBranch2() {
        TestUtils.testDCD("UnreachableBranch2");
    }

    @Test
    public void testDeadAssignment2() {
        TestUtils.testDCD("DeadAssignment2");
    }

    @Test
    public void testMixedDeadCode() {
        TestUtils.testDCD("MixedDeadCode");
    }

    @Test
    public void testLoops() {
        TestUtils.testDCD("Loops");
    }

    @Test
    public void testNotDead() {
        TestUtils.testDCD("NotDead");
    }
}
