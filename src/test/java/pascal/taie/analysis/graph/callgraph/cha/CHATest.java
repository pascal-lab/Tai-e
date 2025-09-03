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

package pascal.taie.analysis.graph.callgraph.cha;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pascal.taie.Main;
import pascal.taie.World;
import pascal.taie.analysis.Tests;
import pascal.taie.analysis.graph.callgraph.CallGraph;
import pascal.taie.analysis.graph.callgraph.CallGraphBuilder;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JMethod;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class CHATest {

    private static final String CLASS_PATH = "src/test/resources/cha/";

    static void test(String mainClass) {
        Tests.testMain(mainClass, CLASS_PATH, "cg", "algorithm:cha");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "StaticCall",
            "VirtualCall",
            "Interface",
            "AbstractMethod",
    })
    void testFull(String mainClass) {
        test(mainClass);
    }

    @Test
    void testArray() {
        Main.main("-pp",
                "-cp", CLASS_PATH,
                "-m", "Array",
                "-a", "cg=algorithm:cha-full");
        CallGraph<Invoke, JMethod> callGraph = World.get().getResult(CallGraphBuilder.ID);
        JMethod main = World.get().getMainMethod();
        List<Invoke> invokes = main.getIR().invokes(false).toList();
        assertEquals(2, invokes.size());
        assertFalse(callGraph.getCalleesOf(invokes.get(0)).isEmpty());
        assertFalse(callGraph.getCalleesOf(invokes.get(1)).isEmpty());
    }

}
