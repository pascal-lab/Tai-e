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
import pascal.taie.analysis.Tests;

public class CPTest {

    static void testCP(String inputClass) {
        Tests.testDFA(inputClass, "src/test/resources/dataflow/constprop/",
                ConstantPropagation.ID, "edge-transfer:false");
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
