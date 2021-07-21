/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2020-- Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020-- Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * Tai-e is only for educational and academic purposes,
 * and any form of commercial use is disallowed.
 * Distribution of Tai-e is disallowed without the approval.
 */

package pascal.taie.analysis.pta.plugin.reflection;

import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.ArrayIndex;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSManager;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.solver.PointerFlowEdge;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.Plugin;
import pascal.taie.analysis.pta.plugin.util.Model;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.MapUtils;

import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public class ReflectionAnalysis implements Plugin {

    private Model classModel;

    private MetaObjModel metaObjModel;

    private Model reflectiveActionModel;

    private Solver solver;

    private CSManager csManager;

    private final Map<Var, Set<ReflectiveCallEdge>> reflectiveArgs = MapUtils.newMap();

    @Override
    public void setSolver(Solver solver) {
        classModel = new ClassModel(solver);
        if (solver.getOptions().getString("reflection-log") != null) {
            metaObjModel = new LogBasedModel(solver);
        } else {
            metaObjModel = new StringBasedModel(solver);
        }
        reflectiveActionModel = new ReflectiveActionModel(solver);
        this.solver = solver;
        csManager = solver.getCSManager();
    }

    @Override
    public void onNewMethod(JMethod method) {
        method.getIR().getStmts()
                .stream()
                .filter(s -> s instanceof Invoke)
                .map(s -> (Invoke) s)
                .filter(Predicate.not(Invoke::isDynamic))
                .forEach(invoke -> {
                    classModel.handleNewInvoke(invoke);
                    metaObjModel.handleNewInvoke(invoke);
                    reflectiveActionModel.handleNewInvoke(invoke);
                });
    }

    @Override
    public void onNewPointsToSet(CSVar csVar, PointsToSet pts) {
        if (classModel.isRelevantVar(csVar.getVar())) {
            classModel.handleNewPointsToSet(csVar, pts);
        }
        if (metaObjModel.isRelevantVar(csVar.getVar())) {
            metaObjModel.handleNewPointsToSet(csVar, pts);
        }
        if (reflectiveActionModel.isRelevantVar(csVar.getVar())) {
            reflectiveActionModel.handleNewPointsToSet(csVar, pts);
        }
        Set<ReflectiveCallEdge> edges = reflectiveArgs.get(csVar.getVar());
        if (edges != null) {
            edges.forEach(edge -> passReflectiveArgs(edge, pts));
        }
    }

    @Override
    public void onNewCSMethod(CSMethod csMethod) {
        metaObjModel.handleNewCSMethod(csMethod);
    }

    @Override
    public void onNewCallEdge(Edge<CSCallSite, CSMethod> edge) {
        if (edge instanceof ReflectiveCallEdge) {
            ReflectiveCallEdge refEdge = (ReflectiveCallEdge) edge;
            Context callerCtx = refEdge.getCallSite().getContext();
            // pass argument
            Var args = refEdge.getArgs();
            if (args != null) {
                CSVar csArgs = csManager.getCSVar(callerCtx, args);
                passReflectiveArgs(refEdge, solver.getPointsToSetOf(csArgs));
                // record args for later-arrive array objects
                MapUtils.addToMapSet(reflectiveArgs, args, refEdge);
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
                    solver.addPFGEdge(csRet, csResult, PointerFlowEdge.Kind.RETURN);
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
                    solver.addPFGEdge(elems, csParam, paramType,
                            PointerFlowEdge.Kind.PARAMETER_PASSING);
                }
            });
        });
    }

    /**
     * TODO: merge with SolverImpl.isConcerned(Exp)
     */
    private static boolean isConcerned(Type type) {
        return type instanceof ClassType || type instanceof ArrayType;
    }
}
