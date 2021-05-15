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
import pascal.taie.World;
import pascal.taie.analysis.exception.ThrowAnalysis;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.ir.IR;
import pascal.taie.language.classes.JClass;

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
        Main.buildWorld("-pp", "-cp", "test-resources/basic", "-m", main);
        JClass c = World.getClassHierarchy().getClass(main);
        ThrowAnalysis throwAnalysis = new ThrowAnalysis(
                new AnalysisConfig(ThrowAnalysis.ID, "exception", exception));
        CFGBuilder builder = new CFGBuilder(
                new AnalysisConfig(CFGBuilder.ID,
                        "exception", exception, "dump", true));
        c.getDeclaredMethods().forEach(m -> {
            IR ir = m.getIR();
            ir.storeResult(throwAnalysis.getId(), throwAnalysis.analyze(ir));
            builder.analyze(ir);
        });
    }
}
