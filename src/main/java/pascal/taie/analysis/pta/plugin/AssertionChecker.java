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

package pascal.taie.analysis.pta.plugin;

import pascal.taie.World;
import pascal.taie.analysis.graph.callgraph.CallGraph;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.Maps;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class AssertionChecker implements Plugin {

    private static final String PTA_ASSERT = "PTAAssert";

    private Solver solver;

    private ClassHierarchy hierarchy;

    private Map<JMethod, Consumer<Invoke>> checkers;

    private PointerAnalysisResult pta;

    private CallGraph<Invoke, JMethod> callGraph;

    private List<Invoke> failures;

    public static boolean isEnablePTAAssertion() {
        return World.get().getClassHierarchy().getClass(PTA_ASSERT) != null;
    }

    @Override
    public void setSolver(Solver solver) {
        this.solver = solver;
    }

    @Override
    public void onFinish() {
        hierarchy = solver.getHierarchy();
        registerCheckers();
        pta = solver.getResult();
        callGraph = pta.getCallGraph();
        failures = new ArrayList<>();
        for (JMethod assertApi : checkers.keySet()) {
            for (Invoke invoke : callGraph.getCallersOf(assertApi)) {
                checkAssertion(invoke, assertApi);
            }
        }
        if (!failures.isEmpty()) {
            String message = "Pointer analysis assertion failures:\n" +
                    failures.stream()
                            .map(String::valueOf)
                            .collect(Collectors.joining("\n"));
            throw new AssertionError(message);
        }
    }

    private void registerCheckers() {
        checkers = Maps.newLinkedHashMap();
    }

    /**
     * Registers a checker.
     */
    private void register(String assertApiSig, Consumer<Invoke> checker) {
        JMethod assertApi = hierarchy.getMethod(assertApiSig);
        checkers.put(assertApi, checker);
    }

    private void checkAssertion(Invoke invoke, JMethod assertApi) {
        checkers.get(assertApi).accept(invoke);
    }
}
