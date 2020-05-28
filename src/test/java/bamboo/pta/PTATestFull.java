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

package bamboo.pta;

import org.junit.Test;

import static bamboo.TestUtils.testPTA;

public class PTATestFull {

    @Test
    public void testNew() {
        testPTA("New");
    }

    @Test
    public void testAssign() {
        testPTA("Assign");
    }

    @Test
    public void testStoreLoad() {
        testPTA("StoreLoad");
    }

    @Test
    public void testCall() {
        testPTA("Call");
    }

    @Test
    public void testAssign2() {
        testPTA("Assign2");
    }

    @Test
    public void testInstanceField() {
        testPTA("InstanceField");
    }

    @Test
    public void testInstanceField2() {
        testPTA("InstanceField2");
    }

    @Test
    public void testCallParamRet() {
        testPTA("CallParamRet");
    }

    @Test
    public void testCallField() {
        testPTA("CallField");
    }
}
