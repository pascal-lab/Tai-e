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
import pascal.taie.analysis.NewTestUtils;

public class DCDTestFull {

    @Test
    public void testControlFlowUnreachable() {
        NewTestUtils.testDCD("ControlFlowUnreachable");
    }

    @Test
    public void testUnreachableBranch() {
        NewTestUtils.testDCD("UnreachableBranch");
    }

    @Test
    public void testDeadAssignment() {
        NewTestUtils.testDCD("DeadAssignment");
    }

    @Test
    public void testControlFlowUnreachable2() {
        NewTestUtils.testDCD("ControlFlowUnreachable2");
    }

    @Test
    public void testUnreachableBranch2() {
        NewTestUtils.testDCD("UnreachableBranch2");
    }

    @Test
    public void testDeadAssignment2() {
        NewTestUtils.testDCD("DeadAssignment2");
    }

    @Test
    public void testMixedDeadCode() {
        NewTestUtils.testDCD("MixedDeadCode");
    }

    @Test
    public void testLoops() {
        NewTestUtils.testDCD("Loops");
    }

    @Test
    public void testNotDead() {
        NewTestUtils.testDCD("NotDead");
    }
}
