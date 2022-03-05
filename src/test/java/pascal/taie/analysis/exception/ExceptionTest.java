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
import pascal.taie.ir.IR;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ClassType;
import pascal.taie.util.collection.MultiMap;

import java.util.Set;

public class ExceptionTest {

    private static final String CP = "src/test/resources/basic";

    private static final String MAIN = "Exceptions";

    @Test
    public void testCatchImplicit() {
        test("explicit" /*"all"*/, "implicitCaught", "implicitUncaught");
    }

    @Test
    public void testCatchThrow() {
        test("all", "throwCaught", "throwUncaught", "nestedThrowCaught");
    }

    @Test
    public void testCatchDeclared() {
        test("explicit", "declaredCaught", "declaredUncaught");
    }

    private static void test(String exception, String... methodNames) {
        String[] args = new String[]{
                "-pp", "-cp", CP, "-m", MAIN,
                "-a", ThrowAnalysis.ID + "=exception:" + exception
        };
        Main.main(args);
        JClass c = World.get().getClassHierarchy().getClass(MAIN);
        for (String methodName : methodNames) {
            JMethod m = c.getDeclaredMethod(methodName);
            System.out.println(m);
            IR ir = m.getIR();
            ThrowResult throwResult = ir.getResult(ThrowAnalysis.ID);
            CatchResult result = CatchAnalysis.analyze(ir, throwResult);
            ir.forEach(stmt -> {
                MultiMap<Stmt, ClassType> caught = result.getCaughtOf(stmt);
                Set<ClassType> uncaught = result.getUncaughtOf(stmt);
                if (!caught.isEmpty() || !uncaught.isEmpty()) {
                    System.out.printf("%s(@L%d)%n", stmt, stmt.getLineNumber());
                    if (!caught.isEmpty()) {
                        System.out.println("Caught exceptions:");
                        caught.forEachSet((s, es) ->
                                System.out.printf("%s(@L%d): %s%n", s, s.getLineNumber(), es));
                    }
                    if (!uncaught.isEmpty()) {
                        System.out.println("Uncaught exceptions: " + uncaught);
                    }
                    System.out.println();
                }
            });
            System.out.println("------------------------------");
        }
    }
}
