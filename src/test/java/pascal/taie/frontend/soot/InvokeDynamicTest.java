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

package pascal.taie.frontend.soot;

import org.junit.Test;
import pascal.taie.Main;
import pascal.taie.World;
import pascal.taie.ir.IRPrinter;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;

public class InvokeDynamicTest {

    @Test
    public void testFunction() {
        final String main = "Function";
        Main.buildWorld("-cp", "test-resources/ir", "-m", main,
                "--test-mode");
        JClass mainClass = World.getClassHierarchy().getClass(main);
        JMethod mainMethod = mainClass.getDeclaredMethod("main");
        IRPrinter.print(mainMethod.getIR(), System.out);
    }
}
