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
import pascal.taie.analysis.Tests;
import pascal.taie.analysis.dataflow.inter.InterConstantPropagation;

public class InterCPAliasTest {

    private static final String CLASS_PATH = "src/test/resources/dataflow/constprop/alias";

    void test(String mainClass) {
        Tests.testMain(mainClass, CLASS_PATH, InterConstantPropagation.ID,
                "edge-refine:false;alias-aware:true",
                "-a", "pta=cs:2-obj;implicit-entries:false"
                //, "-a", "icfg=dump:true" // <-- uncomment this code if you want
                // to output ICFGs for the test cases
        );
    }

    @Test
    public void testArray() {
        test("Array");
    }

    @Test
    public void testArrayInter2() {
        test("ArrayInter2");
    }

    @Test
    public void testArrayLoops() {
        test("ArrayLoops");
    }

    @Test
    public void testInstanceField() {
        test("InstanceField");
    }

    @Test
    public void testMultiStores() {
        test("MultiStores");
    }

    @Test
    public void testInterprocedural2() {
        test("Interprocedural2");
    }

    @Test
    public void testObjSens() {
        test("ObjSens");
    }

    @Test
    public void testStaticField() {
        test("StaticField");
    }

    @Test
    public void testStaticFieldMultiStores() {
        test("StaticFieldMultiStores");
    }
}
