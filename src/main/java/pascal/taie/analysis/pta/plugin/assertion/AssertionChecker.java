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

package pascal.taie.analysis.pta.plugin.assertion;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.Plugin;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.TypeSystem;
import pascal.taie.util.collection.Maps;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Implements a mechanism for checking pointer analysis assertions.
 * This mechanism is used for testing pointer analysis.
 */
public class AssertionChecker implements Plugin {

    private static final Logger logger = LogManager.getLogger(AssertionChecker.class);

    /**
     * Name of the stub class that provides assertion APIs.
     */
    static final String PTA_ASSERT = "PTAAssert";

    private Solver solver;

    private JClass ptaAssert;

    @Override
    public void setSolver(Solver solver) {
        this.solver = solver;
        this.ptaAssert = solver.getHierarchy().getClass(PTA_ASSERT);
    }

    @Override
    public void onStart() {
        if (ptaAssert != null) {
            ptaAssert.getDeclaredMethods().forEach(solver::addIgnoredMethod);
        }
    }

    @Override
    public void onFinish() {
        if (ptaAssert == null) {
            logger.warn("class '{}' is not loaded, failed to enable {}",
                    PTA_ASSERT, AssertionChecker.class.getSimpleName());
            return;
        }
        Map<JMethod, Checker> checkers = getCheckers(solver.getHierarchy());
        List<Result> failures = checkAssertions(solver, checkers);
        processFailures(failures);
    }

    private static Map<JMethod, Checker> getCheckers(ClassHierarchy hierarchy) {
        Map<JMethod, Checker> checkers = Maps.newLinkedHashMap();
        for (var value : Checkers.values()) {
            JMethod assertApi = hierarchy.getMethod(value.getApi());
            checkers.put(assertApi, value.getChecker());
        }
        return checkers;
    }

    private static List<Result> checkAssertions(Solver solver, Map<JMethod, Checker> checkers) {
        PointerAnalysisResult pta = solver.getResult();
        ClassHierarchy hierarchy = solver.getHierarchy();
        TypeSystem typeSystem = solver.getTypeSystem();
        List<Result> failures = new ArrayList<>();
        for (JMethod assertApi : checkers.keySet()) {
            for (Invoke invoke : pta.getCallGraph().getCallersOf(assertApi)) {
                Checker checker = checkers.get(assertApi);
                Result result = checker.check(invoke, pta, hierarchy, typeSystem);
                if (!result.failures().isEmpty()) {
                    failures.add(result);
                }
            }
        }
        return failures;
    }

    private static void processFailures(List<Result> failures) {
        if (!failures.isEmpty()) {
            StringBuilder msg = new StringBuilder("Pointer analysis assertion failures:\n");
            failures.forEach(result -> {
                msg.append(result.invoke()).append('\n');
                msg.append("  assertion: ").append(result.assertion()).append('\n');
                msg.append("  failures:\n");
                result.failures().forEach((elem, givenResult) ->
                        msg.append(String.format("    %s -> %s\n", elem, givenResult)));
            });
            throw new AssertionError(msg);
        }
    }
}
