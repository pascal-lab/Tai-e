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

package pascal.taie.analysis.pta;

import org.junit.Test;
import pascal.taie.analysis.Tests;

/**
 * Tests basic functionalities of pointer analysis
 */
public class BasicTest {

    static final String DIR = "basic";

    // Tests for handling basic pointer analysis statements
    @Test
    public void testNew() {
        Tests.testPTA(DIR, "New");
    }

    @Test
    public void testAssign() {
        Tests.testPTA(DIR, "Assign");
    }

    @Test
    public void testStoreLoad() {
        Tests.testPTA(DIR, "StoreLoad");
    }

    @Test
    public void testCall() {
        Tests.testPTA(DIR, "Call");
    }

    @Test
    public void testAssign2() {
        Tests.testPTA(DIR, "Assign2");
    }

    @Test
    public void testInstanceField() {
        Tests.testPTA(DIR, "InstanceField");
    }

    @Test
    public void testInstanceField2() {
        Tests.testPTA(DIR, "InstanceField2");
    }

    @Test
    public void testCallParamRet() {
        Tests.testPTA(DIR, "CallParamRet");
    }

    @Test
    public void testCallField() {
        Tests.testPTA(DIR, "CallField");
    }

    @Test
    public void testStaticCall() {
        Tests.testPTA(DIR, "StaticCall");
    }

    @Test
    public void testMergeParam() {
        Tests.testPTA(DIR, "MergeParam");
    }

    @Test
    public void testLinkedQueue() {
        Tests.testPTA(DIR, "LinkedQueue");
    }

    @Test
    public void testRedBlackBST() {
        Tests.testPTA(DIR, "RedBlackBST");
    }

    @Test
    public void testMultiReturn() {
        Tests.testPTA(DIR, "MultiReturn");
    }

    @Test
    public void testDispatch() {
        Tests.testPTA(DIR, "Dispatch");
    }

    @Test
    public void testDispatch2() {
        Tests.testPTA(DIR, "Dispatch2");
    }

    @Test
    public void testInterface() {
        Tests.testPTA(DIR, "Interface");
    }

    @Test
    public void testRecursion() {
        Tests.testPTA(DIR, "Recursion");
    }

    @Test
    public void testCycle() {
        Tests.testPTA(DIR, "Cycle");
    }

    @Test
    public void testComplexAssign() {
        Tests.testPTA(DIR, "ComplexAssign");
    }
}
