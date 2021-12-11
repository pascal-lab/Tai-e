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

package pascal.taie.analysis.dataflow.analysis.constprop;

import org.junit.Test;

public class InterCPTestFull extends InterCPTest {

    @Test
    public void testArgRet() {
        test("ArgRet");
    }

    @Test
    public void testCall() {
        test("Call");
    }
    
    @Test
    public void testDeadLoop() {
        test("DeadLoop");
    }

    @Test
    public void testFloatArg() {
        test("FloatArg");
    }

    @Test
    public void testMultiReturn() {
        test("MultiReturn");
    }

    @Test
    public void testCharArgs() {
        test("CharArgs");
    }
    
    @Test
    public void testRedBlackBST() {
        test("RedBlackBST");
    }

    @Test
    public void testPlusPlus() {
        test("PlusPlus");
    }
}
