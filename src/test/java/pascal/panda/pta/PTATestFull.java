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

package pascal.panda.pta;

import org.junit.Test;
import pascal.panda.TestUtils;

public class PTATestFull {

    @Test
    public void testNew() {
        TestUtils.testPTA("New");
    }

    @Test
    public void testAssign() {
        TestUtils.testPTA("Assign");
    }

    @Test
    public void testStoreLoad() {
        TestUtils.testPTA("StoreLoad");
    }

    @Test
    public void testCall() {
        TestUtils.testPTA("Call");
    }

    @Test
    public void testAssign2() {
        TestUtils.testPTA("Assign2");
    }

    @Test
    public void testInstanceField() {
        TestUtils.testPTA("InstanceField");
    }

    @Test
    public void testInstanceField2() {
        TestUtils.testPTA("InstanceField2");
    }

    @Test
    public void testCallParamRet() {
        TestUtils.testPTA("CallParamRet");
    }

    @Test
    public void testCallField() {
        TestUtils.testPTA("CallField");
    }
}
