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

package pascal.taie.analysis.dataflow.analysis;

import org.junit.Test;
import pascal.taie.analysis.Tests;

public class LiveVarTestFull {

    static void testSLV(String inputClass) {
        Tests.testDFA(inputClass, "src/test/resources/dataflow/livevar",
                LiveVariableAnalysis.ID, "strongly:true");
    }

    @Test
    public void testAssign() {
        LiveVarTest.testLV("Assign");
    }

    @Test
    public void testInvoke() {
        LiveVarTest.testLV("Invoke");
    }

    @Test
    public void testBranch() {
        LiveVarTest.testLV("Branch");
    }

    @Test
    public void testLoop() {
        LiveVarTest.testLV("Loop");
    }

    @Test
    public void testBranchLoop() {
        LiveVarTest.testLV("BranchLoop");
    }

    @Test
    public void AnonInner() {
        LiveVarTest.testLV("AnonInner");
    }

    @Test
    public void Array() {
        LiveVarTest.testLV("Array");
    }

    @Test
    public void Field() {
        LiveVarTest.testLV("Field");
    }

    @Test
    public void Graph() {
        LiveVarTest.testLV("Graph");
    }

    @Test
    public void Sort() {
        LiveVarTest.testLV("Sort");
    }

    @Test
    public void ComplexAssign() {
        LiveVarTest.testLV("ComplexAssign");
    }

    @Test
    public void Corner() {
        LiveVarTest.testLV("Corner");
    }

    @Test
    public void Fibonacci() {
        LiveVarTest.testLV("Fibonacci");
    }

    @Test
    public void GaussianElimination() {
        LiveVarTest.testLV("GaussianElimination");
    }

    @Test
    public void Switch() {
        LiveVarTest.testLV("Switch");
    }

    @Test
    public void Reference() {
        LiveVarTest.testLV("Reference");
    }

    @Test
    public void testStronglyAssign() {
        testSLV("StronglyAssign");
    }

    @Test
    public void testStronglyBranchLoop() {
        testSLV("StronglyBranchLoop");
    }
}
