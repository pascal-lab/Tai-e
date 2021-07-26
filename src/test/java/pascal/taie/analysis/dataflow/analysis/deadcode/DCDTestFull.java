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

public class DCDTestFull {

    @Test
    public void testControlFlowUnreachable() {
        DCDTest.testDCD("ControlFlowUnreachable");
    }

    @Test
    public void testUnreachableBranch() {
        DCDTest.testDCD("UnreachableBranch");
    }

    @Test
    public void testDeadAssignment() {
        DCDTest.testDCD("DeadAssignment");
    }

    @Test
    public void testControlFlowUnreachable2() {
        DCDTest.testDCD("ControlFlowUnreachable2");
    }

    @Test
    public void testUnreachableBranch2() {
        DCDTest.testDCD("UnreachableBranch2");
    }

    @Test
    public void testDeadAssignment2() {
        DCDTest.testDCD("DeadAssignment2");
    }

    @Test
    public void testMixedDeadCode() {
        DCDTest.testDCD("MixedDeadCode");
    }

    @Test
    public void testLoops() {
        DCDTest.testDCD("Loops");
    }

    @Test
    public void testNotDead() {
        DCDTest.testDCD("NotDead");
    }
}
