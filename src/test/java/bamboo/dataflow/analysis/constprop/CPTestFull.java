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

package bamboo.dataflow.analysis.constprop;

import org.junit.Test;

import static bamboo.dataflow.analysis.constprop.TestUtils.test;

public class CPTestFull {

    @Test
    public void testSimpleConstant() {
        test("SimpleConstant");
    }

    @Test
    public void testSimpleBinary() {
        test("SimpleBinary");
    }

    @Test
    public void testSimpleBranch() {
        test("SimpleBranch");
    }

    @Test
    public void testSimpleBoolean() {
        test("SimpleBoolean");
    }

    @Test
    public void testBinaryOp() {
        test("BinaryOp");
    }

    @Test
    public void testBranchConstant() {
        test("BranchConstant");
    }

    @Test
    public void testBranchNAC() {
        test("BranchNAC");
    }

    @Test
    public void testBranchUndef() {
        test("BranchUndef");
    }

    @Test
    public void testInterprocedural() {
        test("Interprocedural");
    }

    @Test(expected = AssertionError.class)
    public void testBoolean() {
        test("Boolean");
    }

    @Test(expected = AssertionError.class)
    public void testVariousBinaryOp() {
        test("VariousBinaryOp");
    }
}
