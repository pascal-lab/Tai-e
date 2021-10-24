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

public class InterCPTestFull {

    @Test
    public void testExample() {
        InterCPTest.test("Example");
    }

    @Test
    public void testArgRet() {
        InterCPTest.test("ArgRet");
    }

    @Test
    public void testCall() {
        InterCPTest.test("Call");
    }

    @Test
    public void testReference() {
        InterCPTest.test("Reference");
    }

    @Test
    public void testFibonacci() {
        InterCPTest.test("Fibonacci");
    }

    @Test
    public void testDeadLoop() {
        InterCPTest.test("DeadLoop");
    }

    @Test
    public void testFloatArg() {
        InterCPTest.test("FloatArg");
    }

    @Test
    public void testMultiReturn() {
        InterCPTest.test("MultiReturn");
    }

    @Test
    public void testCharArgs() {
        InterCPTest.test("CharArgs");
    }

    @Test
    public void testMultiIntArgs() {
        InterCPTest.test("MultiIntArgs");
    }

    @Test
    public void testRedBlackBST() {
        InterCPTest.test("RedBlackBST");
    }

}
