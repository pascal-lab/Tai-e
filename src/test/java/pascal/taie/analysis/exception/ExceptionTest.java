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

package pascal.taie.analysis.exception;

import org.junit.jupiter.api.Test;
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

    private static final String CP = "src/test/resources/controlflow";

    private static final String MAIN = "Exceptions";

    @Test
    void testCatchImplicit() {
        test("explicit" /*"all"*/, "implicitCaught", "implicitUncaught");
    }

    @Test
    void testCatchThrow() {
        test("all", "throwCaught", "throwUncaught", "nestedThrowCaught");
    }

    @Test
    void testCatchDeclared() {
        test("explicit", "declaredCaught", "declaredUncaught");
    }

    private static void test(String exception, String... methodNames) {
        Main.main(
                "-pp", "-cp", CP, "-m", MAIN,
                "-a", ThrowAnalysis.ID + "=exception:" + exception
        );
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
