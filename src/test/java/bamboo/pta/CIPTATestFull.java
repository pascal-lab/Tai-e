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

import static bamboo.TestUtils.testCIPTA;

public class CIPTATestFull {

    @Test
    public void testNew() {
        testCIPTA("New");
    }

    @Test
    public void testAssign() {
        testCIPTA("Assign");
    }

    @Test
    public void testStoreLoad() {
        testCIPTA("StoreLoad");
    }

    @Test
    public void testCall() {
        testCIPTA("Call");
    }

    @Test
    public void testAssign2() {
        testCIPTA("Assign2");
    }

    @Test
    public void testInstanceField() {
        testCIPTA("InstanceField");
    }

    @Test
    public void testInstanceField2() {
        testCIPTA("InstanceField2");
    }

    @Test
    public void testCallParamRet() {
        testCIPTA("CallParamRet");
    }

    @Test
    public void testCallField() {
        testCIPTA("CallField");
    }
}
