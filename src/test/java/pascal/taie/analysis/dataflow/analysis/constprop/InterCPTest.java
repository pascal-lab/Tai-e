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

public class InterCPTest {

    private static final String CLASS_PATH = "src/test/resources/dataflow/constprop/inter";

    void test(String inputClass) {
        Tests.test(inputClass, CLASS_PATH, InterConstantPropagation.ID,
                "edge-refine:false;alias-aware:false", "-a", "cg=algorithm:cha"
                // , "-a", "icfg=dump:true" // <-- uncomment this code if you want
                                            // to output ICFGs for the test cases
        );
    }

    @Test
    public void testExample() {
        test("Example");
    }

    @Test
    public void testReference() {
        test("Reference");
    }

    @Test
    public void testFibonacci() {
        test("Fibonacci");
    }

    @Test
    public void testMultiIntArgs() {
        test("MultiIntArgs");
    }
}
