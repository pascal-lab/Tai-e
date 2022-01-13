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

package pascal.taie.analysis.graph.callgraph.cha;

import org.junit.Test;

public class CHATestFull extends CHATest {

    @Test
    public void testSpecialCall() {
        test("SpecialCall");
    }

    @Test
    public void testInterface2() {
        test("Interface2");
    }

    @Test
    public void testInterface3() {
        test("Interface3");
    }

    @Test
    public void testRecursion() {
        test("Recursion");
    }

    @Test
    public void testRecursion2() {
        test("Recursion2");
    }

    @Test
    public void testMaxPQ() {
        test("MaxPQ");
    }

    @Test
    public void testLongCallChain() {
        test("LongCallChain");
    }
}
