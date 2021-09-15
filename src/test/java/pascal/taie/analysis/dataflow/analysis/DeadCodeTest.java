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
    public void testControlFlowUnreachable2() {
        testDCD("ControlFlowUnreachable2");
    }

    @Test
    public void testUnreachableIfBranch() {
        testDCD("UnreachableIfBranch");
    }

    @Test
    public void testUnreachableSwitchBranch() {
        testDCD("UnreachableSwitchBranch");
    }

    @Test
    public void testDeadAssignment() {
        testDCD("DeadAssignment");
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
    public void testLoops() {
        testDCD("Loops");
    }

    @Test
    public void testNotDead() {
        testDCD("NotDead");
    }
}
