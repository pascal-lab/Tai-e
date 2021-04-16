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

import org.junit.BeforeClass;
import org.junit.Test;
import pascal.taie.Main;
import pascal.taie.World;
import pascal.taie.analysis.exception.IntraproceduralThrowAnalysis;
import pascal.taie.analysis.exception.ThrowAnalysis;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JClass;

public class CFGTest {

    private static final String MAIN = "CFG";

    @BeforeClass
    public static void buildWorld() {
        Main.buildWorld("-pp", "-cp", "test-resources/ir", "-m", MAIN);
    }

    @Test
    public void testCFG() {
        JClass c = World.getClassHierarchy().getClass(MAIN);
        ThrowAnalysis throwAnalysis = new IntraproceduralThrowAnalysis(true);
        CFGBuilder builder = new CFGBuilder(throwAnalysis);
        c.getDeclaredMethods().forEach(m -> {
            CFG<Stmt> cfg = builder.build(m.getIR());
            CFGDumper.dumpDotFile(cfg, escapeFileName(
                    String.format("output/%s.%s.dot", c, m.getName())));
        });
    }

    private static String escapeFileName(String filePath) {
        return filePath.replaceAll("[\\[\\]<>]", "_");
    }
}
