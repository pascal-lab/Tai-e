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
import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.ArrayIndex;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSManager;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.Plugin;
import pascal.taie.analysis.pta.plugin.util.Model;
import pascal.taie.analysis.pta.plugin.util.Reflections;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.MapEntry;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.Set;

import static pascal.taie.analysis.graph.flowgraph.FlowKind.PARAMETER_PASSING;
import static pascal.taie.analysis.graph.flowgraph.FlowKind.RETURN;

public class ReflectionAnalysis implements Plugin {

    private static final Logger logger = LogManager.getLogger(ReflectionAnalysis.class);

    private static final int IMPRECISE_THRESHOLD = 50;

    private Solver solver;

    private CSManager csManager;

    private InferenceModel inferenceModel;

    @Nullable
    private LogBasedModel logBasedModel;

    private ReflectiveActionModel reflectiveActionModel;

    private AnnotationModel annotationModel;

    private Model othersModel;

    private final MultiMap<Var, ReflectiveCallEdge> reflectiveArgs = Maps.newMultiMap();

    @Override
    public void setSolver(Solver solver) {
        this.solver = solver;
        csManager = solver.getCSManager();

        MetaObjHelper helper = new MetaObjHelper(solver);
        TypeMatcher typeMatcher = new TypeMatcher(solver.getTypeSystem());
        String logPath = solver.getOptions().getString("reflection-log");
        if (logPath != null) {
            logBasedModel = new LogBasedModel(solver, helper, logPath);
        }
        Set<Invoke> invokesWithLog = logBasedModel != null
                ? logBasedModel.getInvokesWithLog() : Set.of();
        String reflection = solver.getOptions().getString("reflection-inference");
        if ("string-constant".equals(reflection)) {
            inferenceModel = new StringBasedModel(solver, helper, invokesWithLog);
        } else if ("solar".equals(reflection)) {
            inferenceModel = new SolarModel(solver, helper, typeMatcher, invokesWithLog);
        } else if (reflection == null) {
            inferenceModel = new DummyModel(solver);
        } else {
            throw new IllegalArgumentException("Illegal reflection option: " + reflection);
        }
        reflectiveActionModel = new ReflectiveActionModel(solver, helper,
                typeMatcher, invokesWithLog);
        annotationModel = new AnnotationModel(solver, helper);
        othersModel = new OthersModel(solver, helper);
    }

    @Override
    public void onNewStmt(Stmt stmt, JMethod container) {
        if (stmt instanceof Invoke invoke) {
            if (!invoke.isDynamic()) {
                inferenceModel.handleNewInvoke(invoke);
                reflectiveActionModel.handleNewInvoke(invoke);
                othersModel.handleNewInvoke(invoke);
            }
        } else {
            inferenceModel.handleNewNonInvokeStmt(stmt);
        }
    }

    @Override
    public void onNewPointsToSet(CSVar csVar, PointsToSet pts) {
        if (inferenceModel.isRelevantVar(csVar.getVar())) {
            inferenceModel.handleNewPointsToSet(csVar, pts);
        }
        if (reflectiveActionModel.isRelevantVar(csVar.getVar())) {
            reflectiveActionModel.handleNewPointsToSet(csVar, pts);
        }
        if (othersModel.isRelevantVar(csVar.getVar())) {
            othersModel.handleNewPointsToSet(csVar, pts);
        }
        reflectiveArgs.get(csVar.getVar())
                .forEach(edge -> passReflectiveArgs(edge, pts));
    }

    @Override
    public void onNewCSMethod(CSMethod csMethod) {
        if (logBasedModel != null) {
            logBasedModel.handleNewCSMethod(csMethod);
        }
    }

    @Override
    public void onUnresolvedCall(CSObj recv, Context context, Invoke invoke) {
        annotationModel.onUnresolvedCall(recv, context, invoke);
    }

    @Override
    public void onNewCallEdge(Edge<CSCallSite, CSMethod> edge) {
        if (edge instanceof ReflectiveCallEdge refEdge) {
            Context callerCtx = refEdge.getCallSite().getContext();
            // pass argument
            Var args = refEdge.getArgs();
            if (args != null) {
                CSVar csArgs = csManager.getCSVar(callerCtx, args);
                passReflectiveArgs(refEdge, solver.getPointsToSetOf(csArgs));
                // record args for later-arrive array objects
                reflectiveArgs.put(args, refEdge);
            }
            // pass return value
            Invoke invoke = refEdge.getCallSite().getCallSite();
            Context calleeCtx = refEdge.getCallee().getContext();
            JMethod callee = refEdge.getCallee().getMethod();
            Var result = invoke.getResult();
            if (result != null && isConcerned(callee.getReturnType())) {
                CSVar csResult = csManager.getCSVar(callerCtx, result);
                callee.getIR().getReturnVars().forEach(ret -> {
                    CSVar csRet = csManager.getCSVar(calleeCtx, ret);
                    solver.addPFGEdge(csRet, csResult, RETURN);
                });
            }
        }
    }

    private void passReflectiveArgs(ReflectiveCallEdge edge, PointsToSet arrays) {
        Context calleeCtx = edge.getCallee().getContext();
        JMethod callee = edge.getCallee().getMethod();
        arrays.forEach(array -> {
            ArrayIndex elems = csManager.getArrayIndex(array);
            callee.getIR().getParams().forEach(param -> {
                Type paramType = param.getType();
                if (isConcerned(paramType)) {
                    CSVar csParam = csManager.getCSVar(calleeCtx, param);
                    solver.addPFGEdge(elems, csParam, PARAMETER_PASSING, paramType);
                }
            });
        });
    }

    private static boolean isConcerned(Type type) {
        return type instanceof ClassType || type instanceof ArrayType;
    }

    @Override
    public void onFinish() {
        if (inferenceModel instanceof SolarModel solar) {
            solar.reportUnsoundCalls();
        }
        reportImpreciseCalls();
    }

    /**
     * Report that may be resolved imprecisely.
     */
    private void reportImpreciseCalls() {
        MultiMap<Invoke, Object> allTargets = collectAllTargets();
        Set<Invoke> invokesWithLog = logBasedModel != null
                ? logBasedModel.getInvokesWithLog() : Set.of();
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
                        String shortName = Reflections.getShortName(invoke);
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
        if (logBasedModel != null) {
            allTargets.putAll(logBasedModel.getForNameTargets());
        }
        allTargets.putAll(inferenceModel.getForNameTargets());
        allTargets.putAll(reflectiveActionModel.getAllTargets());
        return allTargets;
    }
}
