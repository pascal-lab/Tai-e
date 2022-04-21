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

public class ContextSensitivityTestFull extends ContextSensitivityTest {

    // More complex tests
    @Test
    public void testRecursiveObj() {
        Tests.testPTA(DIR, "RecursiveObj", "cs:2-obj");
    }

    @Test
    public void testLongObjContext() {
        Tests.testPTA(DIR, "LongObjContext", "cs:2-obj");
    }

    @Test
    public void testLongCallContext() {
        Tests.testPTA(DIR, "LongCallContext", "cs:2-call");
    }

    @Test
    public void testStaticSelect() {
        Tests.testPTA(DIR, "StaticSelect", "cs:2-obj");
    }

    @Test
    public void testTwoCallOnly() {
        Tests.testPTA(DIR, "TwoCallOnly", "cs:2-call");
    }

    @Test
    public void testObjOnly() {
        Tests.testPTA(DIR, "ObjOnly", "cs:1-obj");
    }

    @Test
    public void testMustUseHeap() {
        Tests.testPTA(DIR, "MustUseHeap", "cs:2-call");
    }

    @Test
    public void testNestedHeap() {
        Tests.testPTA(DIR, "NestedHeap", "cs:2-obj");
    }

    @Test
    public void testCallOnly() {
        Tests.testPTA(DIR, "CallOnly", "cs:1-call");
    }

    @Test
    public void testLinkedQueue() {
        Tests.testPTA(DIR, "LinkedQueue", "cs:2-obj");
    }

    // Tests for handling of non-normal objects
    @Test
    public void testTypeSens() {
        Tests.testPTA(DIR, "TypeSens", "cs:2-type");
    }

    @Test
    public void testSpecialHeapContext() {
        Tests.testPTA(DIR, "SpecialHeapContext", "cs:2-obj");
    }
}
