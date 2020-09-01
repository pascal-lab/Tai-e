/*
 * Bamboo - A Program Analysis Framework for Java
 *
 * Copyright (C)  2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C)  2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Bamboo is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package panda.callgraph.cha;

import org.junit.Test;

import static panda.TestUtils.testCHA;

public class CHATest {

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
}
