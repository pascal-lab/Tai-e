/*
 * Tai'e - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Tai'e is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.taie.callgraph.cha;

import org.junit.Test;
import pascal.taie.TestUtils;

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
