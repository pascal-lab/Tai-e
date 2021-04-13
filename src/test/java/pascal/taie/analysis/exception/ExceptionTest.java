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

    private static final String MAIN = "Exceptions";

    @BeforeClass
    public static void buildWorld() {
        System.setProperty("ENABLE_JIMPLE_OPT", "true");
        Main.buildWorld("-pp", "-cp", "test-resources/graph", "-m", MAIN);
    }

    @AfterClass
    public static void clear() {
        System.clearProperty("ENABLE_JIMPLE_OPT");
    }

    @Test
    public void testThrowAnalysis() {
        JClass c = World.getClassHierarchy().getClass(MAIN);
        ThrowAnalysis throwAnalysis = new DefaultThrowAnalysis(false);
        c.getDeclaredMethods().forEach(m -> {
            System.out.println(m);
            ThrowAnalysis.Result throwResult = throwAnalysis.analyze(m.getIR());
            m.getIR()
                    .getStmts()
                    .forEach(stmt ->
                            System.out.println(stmt + " may throw " +
                                    throwResult.mayThrow(stmt)));
            System.out.println();
        });
    }

    @Test
    public void testCatchAnalysis() {
        JClass c = World.getClassHierarchy().getClass(MAIN);
        ThrowAnalysis throwAnalysis = new DefaultThrowAnalysis(false);
        c.getDeclaredMethods().forEach(m -> {
            System.out.println(m);
            ThrowAnalysis.Result throwResult = throwAnalysis.analyze(m.getIR());
            CatchAnalysis.Result result = CatchAnalysis.analyze(
                    m.getIR(), throwResult);
            m.getIR().getStmts().forEach(stmt -> {
                System.out.println(stmt);
                result.caughtExceptionsOf(stmt).forEach(System.out::println);
                result.uncaughtExceptionsOf(stmt).forEach(System.out::println);
            });
            System.out.println();
        });
    }
}
