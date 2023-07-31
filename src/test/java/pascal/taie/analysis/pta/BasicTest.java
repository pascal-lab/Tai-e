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

import org.junit.jupiter.api.Test;
import pascal.taie.analysis.Tests;

/**
 * Tests basic functionalities of pointer analysis
 */
public class BasicTest {

    static final String DIR = "basic";

    // Tests for handling basic pointer analysis statements
    @Test
    void testNew() {
        Tests.testPTA(DIR, "New");
    }

    @Test
    void testAssign() {
        Tests.testPTA(DIR, "Assign");
    }

    @Test
    void testStoreLoad() {
        Tests.testPTA(DIR, "StoreLoad");
    }

    @Test
    void testCall() {
        Tests.testPTA(DIR, "Call");
    }

    @Test
    void testAssign2() {
        Tests.testPTA(DIR, "Assign2");
    }

    @Test
    void testInstanceField() {
        Tests.testPTA(DIR, "InstanceField");
    }

    @Test
    void testInstanceField2() {
        Tests.testPTA(DIR, "InstanceField2");
    }

    @Test
    void testCallParamRet() {
        Tests.testPTA(DIR, "CallParamRet");
    }

    @Test
    void testCallField() {
        Tests.testPTA(DIR, "CallField");
    }

    @Test
    void testStaticCall() {
        Tests.testPTA(DIR, "StaticCall");
    }

    @Test
    void testMergeParam() {
        Tests.testPTA(DIR, "MergeParam");
    }

    @Test
    void testLinkedQueue() {
        Tests.testPTA(DIR, "LinkedQueue");
    }

    @Test
    void testRedBlackBST() {
        Tests.testPTA(DIR, "RedBlackBST");
    }

    @Test
    void testMultiReturn() {
        Tests.testPTA(DIR, "MultiReturn");
    }

    @Test
    void testDispatch() {
        Tests.testPTA(DIR, "Dispatch");
    }

    @Test
    void testDispatch2() {
        Tests.testPTA(DIR, "Dispatch2");
    }

    @Test
    void testInterface() {
        Tests.testPTA(DIR, "Interface");
    }

    @Test
    void testRecursion() {
        Tests.testPTA(DIR, "Recursion");
    }

    @Test
    void testCycle() {
        Tests.testPTA(DIR, "Cycle");
    }

    @Test
    void testComplexAssign() {
        Tests.testPTA(DIR, "ComplexAssign");
    }
}
