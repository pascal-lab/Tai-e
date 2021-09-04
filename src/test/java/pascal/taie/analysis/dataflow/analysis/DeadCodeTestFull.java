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

public class DeadCodeTestFull {

    @Test
    public void testControlFlowUnreachable() {
        DeadCodeTest.testDCD("ControlFlowUnreachable");
    }

    @Test
    public void testUnreachableBranch() {
        DeadCodeTest.testDCD("UnreachableBranch");
    }

    @Test
    public void testUnreachableCase() {
        DeadCodeTest.testDCD("UnreachableCase");
    }

    @Test
    public void testDeadAssignment() {
        DeadCodeTest.testDCD("DeadAssignment");
    }

    @Test
    public void testControlFlowUnreachable2() {
        DeadCodeTest.testDCD("ControlFlowUnreachable2");
    }

    @Test
    public void testUnreachableBranch2() {
        DeadCodeTest.testDCD("UnreachableBranch2");
    }

    @Test
    public void testDeadAssignment2() {
        DeadCodeTest.testDCD("DeadAssignment2");
    }

    @Test
    public void testMixedDeadCode() {
        DeadCodeTest.testDCD("MixedDeadCode");
    }

    @Test
    public void testLoops() {
        DeadCodeTest.testDCD("Loops");
    }

    @Test
    public void testNotDead() {
        DeadCodeTest.testDCD("NotDead");
    }

    @Test
    public void testLiveAssignments() {
        DeadCodeTest.testDCD("LiveAssignments");
    }
}
