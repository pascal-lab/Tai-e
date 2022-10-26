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

public class CPTestFull extends CPTest {

    @Test
    public void testSimpleBoolean() {
        testCP("SimpleBoolean");
    }

    @Test
    public void testBinaryOp() {
        testCP("BinaryOp");
    }

    @Test
    public void testBranchNAC() {
        testCP("BranchNAC");
    }

    @Test
    public void testBranchUndef() {
        testCP("BranchUndef");
    }

    @Test
    public void testLoop() {
        testCP("Loop");
    }

    @Test
    public void testLogicalOp() {
        testCP("LogicalOp");
    }

    @Test
    public void testDivisionByZero() {
        testCP("DivisionByZero");
    }

    @Test
    public void testMultiplyByZero() {
        testCP("MultiplyByZero");
    }

    @Test
    public void testConditionOp() {
        testCP("ConditionOp");
    }

    @Test
    public void testComparisonOp() {
        testCP("ComparisonOp");
    }
}
