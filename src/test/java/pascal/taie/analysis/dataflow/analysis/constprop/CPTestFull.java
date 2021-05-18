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
import pascal.taie.analysis.NewTestUtils;

public class CPTestFull {

    @Test
    public void testSimpleConstant() {
        NewTestUtils.testCP("SimpleConstant");
    }

    @Test
    public void testSimpleBinary() {
        NewTestUtils.testCP("SimpleBinary");
    }

    @Test
    public void testSimpleBranch() {
        NewTestUtils.testCP("SimpleBranch");
    }

    @Test
    public void testSimpleBoolean() {
        NewTestUtils.testCP("SimpleBoolean");
    }

    @Test
    public void testAssign() {
        NewTestUtils.testCP("Assign");
    }

    @Test
    public void testBinaryOp() {
        NewTestUtils.testCP("BinaryOp");
    }

    @Test
    public void testBranchConstant() {
        NewTestUtils.testCP("BranchConstant");
    }

    @Test
    public void testBranchNAC() {
        NewTestUtils.testCP("BranchNAC");
    }

    @Test
    public void testBranchUndef() {
        NewTestUtils.testCP("BranchUndef");
    }

    @Test
    public void testLoop() {
        NewTestUtils.testCP("Loop");
    }

    @Test
    public void testInterprocedural() {
        NewTestUtils.testCP("Interprocedural");
    }

    @Test(expected = AssertionError.class)
    public void testBoolean() {
        NewTestUtils.testCP("Boolean");
    }

    @Test(expected = AssertionError.class)
    public void testVariousBinaryOp() {
        NewTestUtils.testCP("VariousBinaryOp");
    }
}
