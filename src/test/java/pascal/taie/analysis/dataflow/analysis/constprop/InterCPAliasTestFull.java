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

package pascal.taie.analysis.dataflow.analysis.constprop;

import org.junit.Test;

public class InterCPAliasTestFull extends InterCPAliasTest {

    // Tests instance field
    @Test
    public void testMultiLoads() {
        test("MultiLoads");
    }

    @Test
    public void testMultiObjs() {
        test("MultiObjs");
    }

    @Test
    public void testInterprocedural() {
        test("Interprocedural");
    }

    @Test
    public void testInheritedField() {
        test("InheritedField");
    }

    @Test
    public void testFieldCorner() {
        test("FieldCorner");
    }

    // Tests array
    @Test
    public void testArrayField() {
        test("ArrayField");
    }

    @Test
    public void testArrayInter() {
        test("ArrayInter");
    }

    @Test
    public void testArrayCorner() {
        test("ArrayCorner");
    }

    // Other tests
    @Test
    public void testReference() {
        test("Reference");
    }

    @Test
    public void testObjSens2() {
        test("ObjSens2");
    }

    @Test
    public void testArrayInField() {
        test("ArrayInField");
    }

    @Test
    public void testMaxPQ() {
        test("MaxPQ");
    }
}
