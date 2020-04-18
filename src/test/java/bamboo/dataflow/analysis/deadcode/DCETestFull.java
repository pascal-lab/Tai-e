/*
 * Bamboo - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Bamboo is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package bamboo.dataflow.analysis.deadcode;

import org.junit.Test;

import static bamboo.dataflow.analysis.TestUtils.testDCE;

public class DCETestFull {

    @Test
    public void testSimpleUnreachable() {
        testDCE("SimpleUnreachable");
    }

    @Test
    public void testUnreachableBranch() {
        testDCE("UnreachableBranch");
    }

    @Test
    public void testSimpleUnreachable2() {
        testDCE("SimpleUnreachable2");
    }

    @Test
    public void testDeadAssignment() {
        testDCE("DeadAssignment");
    }

    @Test
    public void testUnreachableBranch2() {
        testDCE("UnreachableBranch2");
    }

    @Test
    public void testDeadAssignment2() {
        testDCE("DeadAssignment2");
    }

    @Test
    public void testMixedDeadCode() {
        testDCE("MixedDeadCode");
    }

    @Test
    public void testNotDead() {
        testDCE("NotDead");
    }
}
