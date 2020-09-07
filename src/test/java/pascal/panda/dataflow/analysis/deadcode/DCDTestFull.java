/*
 * Panda - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Panda is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.panda.dataflow.analysis.deadcode;

import org.junit.Test;
import pascal.panda.TestUtils;

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
