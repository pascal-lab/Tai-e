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
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static pascal.taie.frontend.java.LineNumberAssertions.assertBinaryLines;
import static pascal.taie.frontend.java.LineNumberAssertions.assertCastLines;
import static pascal.taie.frontend.java.LineNumberAssertions.assertCopyLine;
import static pascal.taie.frontend.java.LineNumberAssertions.assertCopyLines;
import static pascal.taie.frontend.java.LineNumberAssertions.assertIfLines;
import static pascal.taie.frontend.java.LineNumberAssertions.assertInvokeLine;
import static pascal.taie.frontend.java.LineNumberAssertions.assertInvokeLines;
import static pascal.taie.frontend.java.LineNumberAssertions.assertLiteralLine;
import static pascal.taie.frontend.java.LineNumberAssertions.assertMonitorEnterLines;
import static pascal.taie.frontend.java.LineNumberAssertions.assertMonitorExitLines;
import static pascal.taie.frontend.java.LineNumberAssertions.assertNopLine;
import static pascal.taie.frontend.java.LineNumberAssertions.assertNullAssignmentLine;
import static pascal.taie.frontend.java.LineNumberAssertions.assertPhiLine;
import static pascal.taie.frontend.java.LineNumberAssertions.assertReturnLines;

public class RealLineNumberTest {

    private static JClass lineNumberClass;

    private static JClass ssaLineNumberClass;

    private static JClass getLineNumberClass(boolean ssa) {
        JClass cached = ssa ? ssaLineNumberClass : lineNumberClass;
        if (cached == null) {
            List<String> args = new ArrayList<>();
            Collections.addAll(args, "-cp", "src/test/resources/world",
                    "-m", "LineNumber");
            if (ssa) {
                args.add("--ssa");
            }
            Main.buildWorld(args.toArray(String[]::new));
            cached = World.get().getClassHierarchy().getClass("LineNumber");
            for (JMethod method : cached.getDeclaredMethods()) {
                if (!method.isAbstract() && !method.isNative()) {
                    method.getIR();
                }
            }
            if (ssa) {
                ssaLineNumberClass = cached;
            } else {
                lineNumberClass = cached;
            }
        }
        return cached;
    }

    @Test
    void lambda() {
        Main.buildWorld("-cp", "src/test/resources/world",
                "-m", "Lambda");
        JMethod main = World.get().getClassHierarchy().getClass("Lambda")
                .getDeclaredMethod("main");
        assertInvokeLine(main, "java.util.List", "of", 5);
        assertInvokeLine(main, "java.util.List", "stream", 6);
        assertInvokeLine(main, "java.util.stream.Stream", "map", 7);
        assertInvokeLine(main, "java.util.stream.Stream", "reduce", 8);
        assertInvokeLine(main, "java.io.PrintStream", "println", 5);
    }

    @Test
    void conditionalExpression() {
        JMethod main = getLineNumberClass(false).getDeclaredMethod("main");
        assertCopyLine(main, main.getIR().getParam(0), 21);
        assertNullAssignmentLine(main, 21);
    }

    @Test
    void ssa() {
        JMethod method = getLineNumberClass(true).getDeclaredMethod("ssa");
        assertPhiLine(method, "flag", 6);
        assertPhiLine(method, "value", 7);
    }

    @Test
    void loop() {
        JMethod method = getLineNumberClass(true).getDeclaredMethod("loop");
        // The cached zero is shared by the explicit initialization and
        // the implicit zero in count > 0, so its synthetic definition has
        // no unique bytecode origin.
        assertLiteralLine(method, "value", 0, -1);
        assertBinaryLines(method, List.of(14, 15));
        assertIfLines(method, List.of(13));
        assertInvokeLines(method, List.of(17, 17));
        assertReturnLines(method, List.of(18));
    }

    @Test
    void emptyStackBlock() {
        JMethod method = getLineNumberClass(true)
                .getDeclaredMethod("emptyConditional");
        assertNopLine(method, 27);
    }

    @Test
    void synchronizedCase() {
        JMethod method = getLineNumberClass(false)
                .getDeclaredMethod("synchronizedCase");
        assertMonitorEnterLines(method, List.of(31, 34));
        assertMonitorExitLines(method, List.of(33, 33, 36, 36));
    }

    /**
     * javac emits the first astore at PC 15 under line 43 and the second
     * astore at PC 27 under line 45 in the LineNumberTable.
     */
    @Test
    void splitStore() {
        JMethod method = getLineNumberClass(false).getDeclaredMethod("splitStore");
        assertCopyLines(method, "value", List.of(43, 45));
    }

    @Test
    void inferredArgumentCastsKeepLine() {
        JMethod method = getLineNumberClass(false)
                .getDeclaredMethod("preciseCompare");
        assertCastLines(method, List.of(56, 56));
    }
}
