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
    public void testConditionOp() {
        testCP("ConditionOp");
    }

    @Test
    public void testComparisonOp() {
        testCP("ComparisonOp");
    }
}
