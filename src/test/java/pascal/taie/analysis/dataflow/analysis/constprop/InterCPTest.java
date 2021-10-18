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
import pascal.taie.analysis.dataflow.inter.InterConstantPropagation;

public class InterCPTest {

    private static final String CLASS_PATH = "src/test/resources/dataflow/constprop/inter";

    private static void test(String inputClass) {
        Tests.testDFA(inputClass, CLASS_PATH, InterConstantPropagation.ID,
                "alias-aware:false", "-a", "cg=algorithm:cha"
                // , "-a", "icfg=dump:true" // <-- uncomment this code if you want
                                            // to output ICFGs for the test cases
        );
    }

    @Test
    public void testExample() {
        test("Example");
    }

    @Test
    public void testArgRet() {
        test("ArgRet");
    }

    @Test
    public void testCall() {
        test("Call");
    }
}
