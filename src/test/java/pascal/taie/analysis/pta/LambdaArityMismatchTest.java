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

package pascal.taie.analysis.pta;

import org.junit.jupiter.api.Test;
import pascal.taie.Main;
import pascal.taie.World;
import pascal.taie.analysis.graph.callgraph.CallGraph;
import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.MultiMapCollector;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A framework model feeds the reflective receiver of {@code Method.invoke} into
 * the callee's {@code this}. Since that parameter is {@code Object}, a lambda can
 * reach {@code this.get(...)} of an unrelated class, and {@code LambdaAnalysis}
 * matches lambda call sites by method name alone.
 */
public class LambdaArityMismatchTest {

    @Test
    void mismatchedLambdaEdgeIsDroppedInsteadOfAbortingAnalysis() {
        Main.main("-cp", "src/test/resources/pta/lambda",
                "-m", "ArityMismatch",
                "-a", "pta=handle-invokedynamic:true;"
                        + "plugins:[pascal.taie.analysis.pta.UnfilteredReflectionPlugin];");
        PointerAnalysisResult ptaResult = World.get().getResult(PointerAnalysis.ID);
        CallGraph<Invoke, JMethod> callGraph = ptaResult.getCallGraph();
        assertNotNull(callGraph);

        JMethod caller = World.get().getClassHierarchy()
                .getMethod("<ArityMismatch: void main(java.lang.String[])>");
        JMethod reflectiveTarget = World.get().getClassHierarchy()
                .getMethod("<ArityMismatch$App: void run()>");
        JMethod lambdaBody = World.get().getClassHierarchy()
                .getMethod("<ArityMismatch: java.lang.String lambda$main$0()>");
        MultiMap<JMethod, JMethod> callers = callGraph.edges().collect(
                MultiMapCollector.get(e -> e.getCallSite().getContainer(), Edge::getCallee));

        assertTrue(callers.contains(caller, reflectiveTarget));
        // The lambda object does not belong here: one argument would be
        // transferred into a zero-parameter body, so the edge is dropped.
        assertFalse(callers.contains(reflectiveTarget, lambdaBody));
    }
}
