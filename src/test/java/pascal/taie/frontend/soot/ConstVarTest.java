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

package pascal.taie.frontend.soot;

import org.junit.jupiter.api.Test;
import pascal.taie.Main;
import pascal.taie.World;
import pascal.taie.ir.IR;
import pascal.taie.ir.IRPrinter;
import pascal.taie.ir.exp.Var;
import pascal.taie.language.classes.JClass;

public class ConstVarTest {

    @Test
    void test() {
        String main = "ConstVar";
        Main.buildWorld("-pp", "-cp", "src/test/resources/world", "--input-classes", main);
        JClass jclass = World.get().getClassHierarchy().getClass(main);
        jclass.getDeclaredMethods().forEach(m -> {
            IR ir = m.getIR();
            IRPrinter.print(ir, System.out);
            ir.getVars()
                    .stream()
                    .filter(Var::isConst)
                    .forEach(v -> System.out.println(v + " -> " + v.getConstValue()));
        });
    }
}
