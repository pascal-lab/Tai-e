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

package pascal.taie.analysis.exception;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import pascal.taie.Main;
import pascal.taie.World;
import pascal.taie.language.classes.JClass;

public class ExceptionTest {

    @BeforeClass
    public static void buildWorld() {
        System.setProperty("ENABLE_JIMPLE_OPT", "true");
        Main.buildWorld(new String[]{
                "-cp", "test-resources/graph",
                "-m", "Exceptions"
        });
    }

    @AfterClass
    public static void clear() {
        System.clearProperty("ENABLE_JIMPLE_OPT");
    }

    @Test
    public void testThrowAnalysis() {
        ThrowAnalysis throwAnalysis = new ThrowAnalysis(true);
        JClass c = World.getClassHierarchy().getClass("Exceptions");
        c.getDeclaredMethods().forEach(m -> {
            System.out.println(m);
            m.getIR()
                    .getStmts()
                    .forEach(stmt ->
                            System.out.println(stmt + " may throw " +
                                    throwAnalysis.mayThrow(stmt)));
            System.out.println();
        });
    }
}
