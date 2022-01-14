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
import pascal.taie.ir.IR;
import pascal.taie.ir.IRPrinter;
import pascal.taie.ir.exp.Var;
import pascal.taie.language.classes.JClass;

public class TempConstVarTest {

    @Test
    public void test() {
        String main = "TempConst";
        Main.buildWorld("-pp", "-cp", "src/test/resources/basic", "-m", main);
        JClass jclass = World.get().getClassHierarchy().getClass(main);
        jclass.getDeclaredMethods().forEach(m -> {
            IR ir = m.getIR();
            IRPrinter.print(ir, System.out);
            ir.getVars()
                    .stream()
                    .filter(Var::isTempConst)
                    .forEach(v -> System.out.println(v + " -> " + v.getTempConstValue()));
        });
    }
}
