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

public class LiveVarTestFull extends LiveVarTest {

    void testSLV(String inputClass) {
        Tests.test(inputClass, "src/test/resources/dataflow/livevar",
                LiveVariableAnalysis.ID, "strongly:true");
    }

    @Test
    public void testAssign() {
        testLV("Assign");
    }

    @Test
    public void testInvoke() {
        testLV("Invoke");
    }

    @Test
    public void testBranch() {
        testLV("Branch");
    }

    @Test
    public void testLoop() {
        testLV("Loop");
    }

    @Test
    public void testBranchLoop() {
        testLV("BranchLoop");
    }

    @Test
    public void AnonInner() {
        testLV("AnonInner");
    }

    @Test
    public void Array() {
        testLV("Array");
    }

    @Test
    public void Field() {
        testLV("Field");
    }

    @Test
    public void Graph() {
        testLV("Graph");
    }

    @Test
    public void Sort() {
        testLV("Sort");
    }

    @Test
    public void ComplexAssign() {
        testLV("ComplexAssign");
    }

    @Test
    public void Corner() {
        testLV("Corner");
    }

    @Test
    public void Fibonacci() {
        testLV("Fibonacci");
    }

    @Test
    public void GaussianElimination() {
        testLV("GaussianElimination");
    }

    @Test
    public void Switch() {
        testLV("Switch");
    }

    @Test
    public void Reference() {
        testLV("Reference");
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
