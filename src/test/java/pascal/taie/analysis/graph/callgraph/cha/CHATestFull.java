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
import pascal.taie.analysis.Tests;

public class CHATestFull {

    @Test
    public void testStaticCall() {
        Tests.testCHA("StaticCall");
    }

    @Test
    public void testSpecialCall() {
        Tests.testCHA("SpecialCall");
    }

    @Test
    public void testVirtualCall() {
        Tests.testCHA("VirtualCall");
    }

    @Test
    public void testInterface() {
        Tests.testCHA("Interface");
    }

    @Test
    public void testInterface2() {
        Tests.testCHA("Interface2");
    }

    @Test
    public void testAbstractMethod() {
        Tests.testCHA("AbstractMethod");
    }

    @Test
    public void testRecursion() {
        Tests.testCHA("Recursion");
    }

    @Test
    public void testLongCallChain() {
        Tests.testCHA("LongCallChain");
    }
}
