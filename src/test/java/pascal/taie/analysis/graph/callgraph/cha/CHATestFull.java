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
import pascal.taie.analysis.TestUtils;

public class CHATestFull {

    @Test
    public void testStaticCall() {
        TestUtils.testCHA("StaticCall");
    }

    @Test
    public void testSpecialCall() {
        TestUtils.testCHA("SpecialCall");
    }

    @Test
    public void testVirtualCall() {
        TestUtils.testCHA("VirtualCall");
    }

    @Test
    public void testInterface() {
        TestUtils.testCHA("Interface");
    }

    @Test
    public void testInterface2() {
        TestUtils.testCHA("Interface2");
    }

    @Test
    public void testAbstractMethod() {
        TestUtils.testCHA("AbstractMethod");
    }

    @Test
    public void testRecursion() {
        TestUtils.testCHA("Recursion");
    }

    @Test
    public void testLongCallChain() {
        TestUtils.testCHA("LongCallChain");
    }
}
