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

package pascal.taie.frontend.java;

import org.junit.jupiter.api.Test;

import pascal.taie.Main;
import pascal.taie.World;
import pascal.taie.ir.IRPrinter;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Comparator;
import java.util.List;

public class TestIRGen {

    private static final String path = "src/test/resources/frontend";

    private static final String outputPath = "output";

    private static final String planPath = path + "/" + "plan/plan.yml";

    private static final List<String> willFailed = List.of(
            "Inner1",
            "Inner2");

    private static final List<String> targets = List.of(
            "AddTest",
            "Arith",
            "Arr",
            "ArrayLength",
            "Assignment",
            "BinaryTree",
            "Call1",
            "Cond",
            "Conversion",
            "ExpTest",
            "ForLoop",
            "If",
            "If2",
            "Inner1",
            "Inner2",
            "InstanceOf",
            "Left",
            "Literal",
            "Locate1",
            "Loop",
            "Obj1",
            "PPExp",
            "SameName",
            "StaticCall",
            "Str",
            "Super",
            "SuperInvocation",
            "Switch",
            "Synchronized",
            "Try1",
            "Try2",
            "Try3",
            "Try4",
            "Try5",
            "Try6",
            "Try7",
            "TypeConv",
            "TypeLiteral",
            "Varargs");

    private static void buildWorld(String mainClass) {
        Main.main(new String[]{"-cp", path, "--input-classes", mainClass, "-a", "cfg"});
    }

    private void outputIr(String klass, String path) {
        buildWorld(klass);
        JClass mainClass = World.get().getClassHierarchy().getClass(klass);
        try (PrintStream fout = new PrintStream(makePath(path, klass))) {
            mainClass.getDeclaredMethods()
                    .stream()
                    .sorted(Comparator.comparing(JMethod::toString))
                    .forEach(m ->
                            IRPrinter.print(m.getIR(), fout));
            fout.println("------------------------------\n");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        World.reset();
    }

    @Test
    public void testIRBuilder() {
        targets.forEach(klass -> {
            outputIr(klass, outputPath);
//            try {
//                File f1 = new File(makePath(path, klass));
//                File f2 = new File(makePath(outputPath, klass));
//
//            } catch (IOException e) {
//                e.printStackTrace();
//                assert false;
//            }
        });
    }


    private String makePath(String dir, String klass) {
        return dir + "/" + klass + ".tir";
    }

}
