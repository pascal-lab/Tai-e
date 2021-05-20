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

package pascal.taie.analysis.graph.cfg;

import org.junit.Test;
import pascal.taie.Main;
import pascal.taie.analysis.exception.ThrowAnalysis;

public class CFGTest {

    @Test
    public void testCFG() {
        test("CFG", "explicit");
    }

    @Test
    public void testException() {
        test("Exceptions", "all");
    }

    private static void test(String main, String exception) {
        String[] args = new String[] {
                "-pp", "-cp", "test-resources/basic", "-m", main,
                "-a", ThrowAnalysis.ID + "=exception:" + exception,
                "-a", CFGBuilder.ID + "=exception:" + exception + ";dump:true"
        };
        Main.main(args);
    }
}
