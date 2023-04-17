/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2022 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2022 Yue Li <yueli@nju.edu.cn>
 *
 * This file is part of Tai-e.
 *
 * Tai-e is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Tai-e is distributed in the hope that it will be useful,but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Tai-e. If not, see <https://www.gnu.org/licenses/>.
 */

package pascal.taie.analysis.dataflow.analysis;

import org.junit.Test;
import pascal.taie.analysis.Tests;

public class LiveVarTestFull extends LiveVarTest {

    void testSLV(String inputClass) {
        Tests.testInput(inputClass, "src/test/resources/dataflow/livevar",
                LiveVariable.ID, "strongly:true");
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
    public void testAnonInner() {
        testLV("AnonInner");
    }

    @Test
    public void testArray() {
        testLV("Array");
    }

    @Test
    public void testField() {
        testLV("Field");
    }

    @Test
    public void testGraph() {
        testLV("Graph");
    }

    @Test
    public void testSort() {
        testLV("Sort");
    }

    @Test
    public void testComplexAssign() {
        testLV("ComplexAssign");
    }

    @Test
    public void testCorner() {
        testLV("Corner");
    }

    @Test
    public void testFibonacci() {
        testLV("Fibonacci");
    }

    @Test
    public void testGaussianElimination() {
        testLV("GaussianElimination");
    }

    @Test
    public void testSwitch() {
        testLV("Switch");
    }

    @Test
    public void testReference() {
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
