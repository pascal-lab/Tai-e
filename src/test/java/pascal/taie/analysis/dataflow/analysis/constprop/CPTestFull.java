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
import pascal.taie.TestUtils;

public class CPTestFull {

    @Test
    public void testSimpleConstant() {
        TestUtils.testCP("SimpleConstant");
    }

    @Test
    public void testSimpleBinary() {
        TestUtils.testCP("SimpleBinary");
    }

    @Test
    public void testSimpleBranch() {
        TestUtils.testCP("SimpleBranch");
    }

    @Test
    public void testSimpleBoolean() {
        TestUtils.testCP("SimpleBoolean");
    }

    @Test
    public void testAssign() {
        TestUtils.testCP("Assign");
    }

    @Test
    public void testBinaryOp() {
        TestUtils.testCP("BinaryOp");
    }

    @Test
    public void testBranchConstant() {
        TestUtils.testCP("BranchConstant");
    }

    @Test
    public void testBranchNAC() {
        TestUtils.testCP("BranchNAC");
    }

    @Test
    public void testBranchUndef() {
        TestUtils.testCP("BranchUndef");
    }

    @Test
    public void testLoop() {
        TestUtils.testCP("Loop");
    }

    @Test
    public void testInterprocedural() {
        TestUtils.testCP("Interprocedural");
    }

    @Test(expected = AssertionError.class)
    public void testBoolean() {
        TestUtils.testCP("Boolean");
    }

    @Test(expected = AssertionError.class)
    public void testVariousBinaryOp() {
        TestUtils.testCP("VariousBinaryOp");
    }
}
