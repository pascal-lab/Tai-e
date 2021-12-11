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

public class DeadCodeTestFull extends DeadCodeTest {

    @Test
    public void testControlFlowUnreachable2() {
        testDCD("ControlFlowUnreachable2");
    }
    
    @Test
    public void testUnreachableIfBranch2() {
        testDCD("UnreachableIfBranch2");
    }

    @Test
    public void testUnreachableSwitchBranch2() {
        testDCD("UnreachableSwitchBranch2");
    }

    @Test
    public void testDeadAssignment2() {
        testDCD("DeadAssignment2");
    }

    @Test
    public void testLiveAssignments() {
        testDCD("LiveAssignments");
    }

    @Test
    public void testMixedDeadCode() {
        testDCD("MixedDeadCode");
    }

    @Test
    public void testNotDead() {
        testDCD("NotDead");
    }

    @Test
    public void testCorner() {
        testDCD("Corner");
    }

    @Test
    public void testAllReachableIfBranch() {
        testDCD("AllReachableIfBranch");
    }

    @Test
    public void testForLoops() {
        testDCD("ForLoops");
    }

    @Test
    public void testArrayField() {
        testDCD("ArrayField");
    }
}
