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

package pascal.taie.analysis.oldpta;

import org.junit.Test;
import pascal.taie.TestUtils;

public class PTATestFull {

    @Test
    public void testNew() {
        TestUtils.testOldPTA("New");
    }

    @Test
    public void testAssign() {
        TestUtils.testOldPTA("Assign");
    }

    @Test
    public void testStoreLoad() {
        TestUtils.testOldPTA("StoreLoad");
    }

    @Test
    public void testCall() {
        TestUtils.testOldPTA("Call");
    }

    @Test
    public void testAssign2() {
        TestUtils.testOldPTA("Assign2");
    }

    @Test
    public void testInstanceField() {
        TestUtils.testOldPTA("InstanceField");
    }

    @Test
    public void testInstanceField2() {
        TestUtils.testOldPTA("InstanceField2");
    }

    @Test
    public void testCallParamRet() {
        TestUtils.testOldPTA("CallParamRet");
    }

    @Test
    public void testCallField() {
        TestUtils.testOldPTA("CallField");
    }
}
