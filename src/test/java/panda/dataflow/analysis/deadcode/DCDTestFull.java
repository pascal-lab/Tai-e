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

package panda.dataflow.analysis.deadcode;

import org.junit.Test;

import static panda.TestUtils.testDCD;

public class DCDTestFull {

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

    @Test
    public void testControlFlowUnreachable2() {
        testDCD("ControlFlowUnreachable2");
    }

    @Test
    public void testUnreachableBranch2() {
        testDCD("UnreachableBranch2");
    }

    @Test
    public void testDeadAssignment2() {
        testDCD("DeadAssignment2");
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
