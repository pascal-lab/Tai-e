/*
 * Bamboo - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Bamboo is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package panda.dataflow.analysis.constprop;

import org.junit.Test;

import static panda.TestUtils.testCP;

public class CPTestFull {

    @Test
    public void testSimpleConstant() {
        testCP("SimpleConstant");
    }

    @Test
    public void testSimpleBinary() {
        testCP("SimpleBinary");
    }

    @Test
    public void testSimpleBranch() {
        testCP("SimpleBranch");
    }

    @Test
    public void testSimpleBoolean() {
        testCP("SimpleBoolean");
    }

    @Test
    public void testAssign() {
        testCP("Assign");
    }

    @Test
    public void testBinaryOp() {
        testCP("BinaryOp");
    }

    @Test
    public void testBranchConstant() {
        testCP("BranchConstant");
    }

    @Test
    public void testBranchNAC() {
        testCP("BranchNAC");
    }

    @Test
    public void testBranchUndef() {
        testCP("BranchUndef");
    }

    @Test
    public void testLoop() {
        testCP("Loop");
    }

    @Test
    public void testInterprocedural() {
        testCP("Interprocedural");
    }

    @Test(expected = AssertionError.class)
    public void testBoolean() {
        testCP("Boolean");
    }

    @Test(expected = AssertionError.class)
    public void testVariousBinaryOp() {
        testCP("VariousBinaryOp");
    }
}
