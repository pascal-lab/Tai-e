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

public class DCDTest {

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
}
