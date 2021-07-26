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

public class CPTestFull {

    @Test
    public void testSimpleConstant() {
        CPTest.testCP("SimpleConstant");
    }

    @Test
    public void testSimpleBinary() {
        CPTest.testCP("SimpleBinary");
    }

    @Test
    public void testSimpleBranch() {
        CPTest.testCP("SimpleBranch");
    }

    @Test
    public void testSimpleBoolean() {
        CPTest.testCP("SimpleBoolean");
    }

    @Test
    public void testAssign() {
        CPTest.testCP("Assign");
    }

    @Test
    public void testBinaryOp() {
        CPTest.testCP("BinaryOp");
    }

    @Test
    public void testBranchConstant() {
        CPTest.testCP("BranchConstant");
    }

    @Test
    public void testBranchNAC() {
        CPTest.testCP("BranchNAC");
    }

    @Test
    public void testBranchUndef() {
        CPTest.testCP("BranchUndef");
    }

    @Test
    public void testLoop() {
        CPTest.testCP("Loop");
    }

    @Test
    public void testInterprocedural() {
        CPTest.testCP("Interprocedural");
    }

    @Test(expected = AssertionError.class)
    public void testBoolean() {
        CPTest.testCP("Boolean");
    }

    @Test(expected = AssertionError.class)
    public void testVariousBinaryOp() {
        CPTest.testCP("VariousBinaryOp");
    }
}
