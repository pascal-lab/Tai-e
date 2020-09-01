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

package panda.callgraph.cha;

import org.junit.Test;

import static panda.TestUtils.testCHA;

public class CHATestFull {

    @Test
    public void testStaticCall() {
        testCHA("StaticCall");
    }

    @Test
    public void testSpecialCall() {
        testCHA("SpecialCall");
    }

    @Test
    public void testVirtualCall() {
        testCHA("VirtualCall");
    }

    @Test
    public void testInterface() {
        testCHA("Interface");
    }

    @Test
    public void testInterface2() {
        testCHA("Interface2");
    }

    @Test
    public void testAbstractMethod() {
        testCHA("AbstractMethod");
    }

    @Test
    public void testRecursion() {
        testCHA("Recursion");
    }

    @Test
    public void testLongCallChain() {
        testCHA("LongCallChain");
    }
}
