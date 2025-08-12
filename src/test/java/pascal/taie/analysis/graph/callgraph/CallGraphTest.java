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

package pascal.taie.analysis.graph.callgraph;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pascal.taie.Main;
import pascal.taie.World;
import pascal.taie.analysis.pta.PointerAnalysis;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.Sets;

import java.util.Collection;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CallGraphTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "New",
            "Assign",
            "StoreLoad",
            "Call",
            "Assign2",
    })
    void test(String mainClass) {
        Main.main("-pp",
                "-cp", "src/test/resources/pta/basic",
                "-cp", "src/test/resources/pta",
                "-m", mainClass,
                "-a", "pta");
        PointerAnalysisResult pta = World.get().getResult(PointerAnalysis.ID);
        CallGraph<CSCallSite, CSMethod> csGraph = pta.getCSCallGraph();
        CallGraph<Invoke, JMethod> ciGraph = pta.getCallGraph();

        assertSizeEquals(csGraph.reachableMethods(), ciGraph.reachableMethods());
        assertSizeEquals(csGraph.entryMethods(), ciGraph.entryMethods());

        assertEquals(csGraph.getNumberOfEdges(), csGraph.edges().count());
        assertEquals(csGraph.getNumberOfMethods(), csGraph.reachableMethods().count());
        assertEquals(csGraph.getNumberOfNodes(), csGraph.getNumberOfMethods());

        assertEquals(ciGraph.getNumberOfEdges(), ciGraph.edges().count());
        assertEquals(ciGraph.getNumberOfMethods(), ciGraph.reachableMethods().count());
        assertEquals(ciGraph.getNumberOfNodes(), ciGraph.getNumberOfMethods());

        for (CSMethod csMethod : csGraph) {
            JMethod method = csMethod.getMethod();
            assertSizeEquals(csGraph.getSuccsOf(csMethod), ciGraph.getSuccsOf(method));
            assertSizeEquals(csGraph.getPredsOf(csMethod), ciGraph.getPredsOf(method));

            assertSizeEquals(csGraph.getCallersOf(csMethod), ciGraph.getCallersOf(method));
            assertSizeEquals(csGraph.getCalleesOfM(csMethod), ciGraph.getCalleesOfM(method));

            for (CSCallSite csCallSite : csGraph.getCallersOf(csMethod)) {
                Invoke callSite = csCallSite.getCallSite();
                assertSizeEquals(csGraph.getCalleesOf(csCallSite), ciGraph.getCalleesOf(callSite));
            }
        }
    }

    private static <T1, T2> void assertSizeEquals(Collection<T1> expected, Collection<T2> actual) {
        // `CSCallGraph.getCalleesOf()` uses `Views.toMappedSet`. Such sets may contain duplicate
        // elements by design. Here we wrap it with another set to avoid duplicates.
        assertEquals(Sets.newSet(expected).size(), Sets.newSet(actual).size());
    }

    private static <T1, T2> void assertSizeEquals(Stream<T1> expected, Stream<T2> actual) {
        assertEquals(expected.distinct().count(), actual.distinct().count());
    }
}
