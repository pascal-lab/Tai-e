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

public class CPTest {

    void testCP(String inputClass) {
        Tests.test(inputClass, "src/test/resources/dataflow/constprop/",
                ConstantPropagation.ID, "edge-refine:false");
    }

    @Test
    public void testAssign() {
        testCP("Assign");
    }

    @Test
    public void testSimpleConstant() {
        testCP("SimpleConstant");
    }

    @Test
    public void testSimpleBinary() {
        testCP("SimpleBinary");
    }

    @Test
    public void testSimpleBranch() {
        testCP("SimpleBranch");
    }

    @Test
    public void testSimpleChar() {
        testCP("SimpleChar");
    }

    @Test
    public void testBranchConstant() {
        testCP("BranchConstant");
    }

    @Test
    public void testInterprocedural() {
        testCP("Interprocedural");
    }
}
