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

package pascal.taie.analysis.pta.plugin.reflection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.CompositePlugin;
import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.util.collection.MapEntry;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;

import java.util.Comparator;
import java.util.Set;

public class ReflectionAnalysis extends CompositePlugin {

    private static final Logger logger = LogManager.getLogger(ReflectionAnalysis.class);

    private static final int IMPRECISE_THRESHOLD = 50;

    private LogBasedModel logBasedModel;

    private InferenceModel inferenceModel;

    private ReflectiveActionModel reflectiveActionModel;

    /**
     * @return short name of reflection API in given {@link Invoke}.
     */
    public static String getShortName(Invoke invoke) {
        MethodRef ref = invoke.getMethodRef();
        String className = ref.getDeclaringClass().getSimpleName();
        String methodName = ref.getName();
        return className + "." + methodName;
    }

    @Override
    public void setSolver(Solver solver) {
        MetaObjHelper helper = new MetaObjHelper(solver);
        TypeMatcher typeMatcher = new TypeMatcher(solver.getTypeSystem());
        String logPath = solver.getOptions().getString("reflection-log");
        logBasedModel = new LogBasedModel(solver, helper, logPath);
        Set<Invoke> invokesWithLog = logBasedModel.getInvokesWithLog();
        String reflection = solver.getOptions().getString("reflection-inference");
        if ("string-constant".equals(reflection)) {
            inferenceModel = new StringBasedModel(solver, helper, invokesWithLog);
        } else if ("solar".equals(reflection)) {
            inferenceModel = new SolarModel(solver, helper, typeMatcher, invokesWithLog);
        } else if (reflection == null) {
            inferenceModel = InferenceModel.getDummy(solver);
        } else {
            throw new IllegalArgumentException("Illegal reflection option: " + reflection);
        }
        reflectiveActionModel = new ReflectiveActionModel(solver, helper,
                typeMatcher, invokesWithLog);

        addPlugin(logBasedModel,
                inferenceModel,
                reflectiveActionModel,
                new AnnotationModel(solver, helper),
                new OthersModel(solver, helper));
    }

    @Override
    public void onFinish() {
        super.onFinish();
        reportImpreciseCalls();
    }

    /**
     * Report that may be resolved imprecisely.
     */
    private void reportImpreciseCalls() {
        MultiMap<Invoke, Object> allTargets = collectAllTargets();
        Set<Invoke> invokesWithLog = logBasedModel.getInvokesWithLog();
        var impreciseCalls = allTargets.keySet()
                .stream()
                .map(invoke -> new MapEntry<>(invoke, allTargets.get(invoke)))
                .filter(e -> !invokesWithLog.contains(e.getKey()))
                .filter(e -> e.getValue().size() > IMPRECISE_THRESHOLD)
                .toList();
        if (!impreciseCalls.isEmpty()) {
            logger.info("Imprecise reflective calls:");
            impreciseCalls.stream()
                    .sorted(Comparator.comparingInt(
                            (MapEntry<Invoke, Set<Object>> e) -> -e.getValue().size())
                            .thenComparing(MapEntry::getKey))
                    .forEach(e -> {
                        Invoke invoke = e.getKey();
                        String shortName = getShortName(invoke);
                        logger.info("[{}]{}, #targets: {}",
                                shortName, invoke, e.getValue().size());
                    });
        }
    }

    /**
     * Collects all reflective targets resolved by reflection analysis.
     */
    private MultiMap<Invoke, Object> collectAllTargets() {
        MultiMap<Invoke, Object> allTargets = Maps.newMultiMap();
        allTargets.putAll(logBasedModel.getForNameTargets());
        allTargets.putAll(inferenceModel.getForNameTargets());
        allTargets.putAll(reflectiveActionModel.getAllTargets());
        return allTargets;
    }
}
