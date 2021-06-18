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

    private Model reflectiveActionModel;

    private Solver solver;

    private CSManager csManager;

    private final Map<Var, Set<ReflectiveCallEdge>> reflectiveArgs = MapUtils.newMap();

    @Override
    public void setSolver(Solver solver) {
        classModel = new ClassModel(solver);
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
                    reflectiveActionModel.handleNewInvoke(invoke);
                });
    }

    @Override
    public void onNewPointsToSet(CSVar csVar, PointsToSet pts) {
        if (classModel.isRelevantVar(csVar.getVar())) {
            classModel.handleNewPointsToSet(csVar, pts);
        }
        if (reflectiveActionModel.isRelevantVar(csVar.getVar())) {
            reflectiveActionModel.handleNewPointsToSet(csVar, pts);
        }
    }

    @Override
    public void onNewCallEdge(Edge<CSCallSite, CSMethod> edge) {
        if (edge instanceof ReflectiveCallEdge) {
            System.out.println(edge);
            // pass return value
            Context callerCtx = edge.getCallSite().getContext();
            Invoke invoke = edge.getCallSite().getCallSite();
            Context calleeCtx = edge.getCallee().getContext();
            JMethod callee = edge.getCallee().getMethod();
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

    /**
     * TODO: merge with {@link pascal.taie.analysis.pta.core.solver.SolverImpl#isConcerned}
     */
    private static boolean isConcerned(Type type) {
        return type instanceof ClassType || type instanceof ArrayType;
    }
}
