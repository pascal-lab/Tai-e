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

import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.ArrayIndex;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSManager;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
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
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;

import static pascal.taie.analysis.pta.core.solver.PointerFlowEdge.Kind.PARAMETER_PASSING;
import static pascal.taie.analysis.pta.core.solver.PointerFlowEdge.Kind.RETURN;

public class ReflectionAnalysis implements Plugin {

    private Model classModel;

    private MetaObjModel metaObjModel;

    private Model reflectiveActionModel;

    private Solver solver;

    private CSManager csManager;

    private final MultiMap<Var, ReflectiveCallEdge> reflectiveArgs = Maps.newMultiMap();

    @Override
    public void setSolver(Solver solver) {
        this.solver = solver;
        classModel = new ClassModel(solver);
        String reflection = solver.getOptions().getString("reflection");
        if ("string-constant".equals(reflection)) {
            metaObjModel = new StringBasedModel(solver);
        } else if ("log".equals(reflection)) {
            metaObjModel = new LogBasedModel(solver);
        } else {
            throw new IllegalArgumentException("Illegal reflection option: " + reflection);
        }
        reflectiveActionModel = new ReflectiveActionModel(solver);
        csManager = solver.getCSManager();
    }

    @Override
    public void onNewMethod(JMethod method) {
        method.getIR()
                .invokes(false)
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
        reflectiveArgs.get(csVar.getVar())
                .forEach(edge -> passReflectiveArgs(edge, pts));
    }

    @Override
    public void onNewCSMethod(CSMethod csMethod) {
        metaObjModel.handleNewCSMethod(csMethod);
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

    /**
     * TODO: merge with DefaultSolver.isConcerned(Exp)
     */
    private static boolean isConcerned(Type type) {
        return type instanceof ClassType || type instanceof ArrayType;
    }
}
