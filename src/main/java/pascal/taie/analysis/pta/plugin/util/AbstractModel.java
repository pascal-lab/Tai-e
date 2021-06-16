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

package pascal.taie.analysis.pta.plugin.util;

import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSManager;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.heap.HeapModel;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.exp.InvokeExp;
import pascal.taie.ir.exp.InvokeInstanceExp;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.TriConsumer;
import pascal.taie.util.collection.MapUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Provides common functionalities for implementing API models.
 */
public abstract class AbstractModel implements Model {

    protected final Solver solver;

    protected final ClassHierarchy hierarchy;

    protected final CSManager csManager;

    protected final HeapModel heapModel;

    /**
     * Default heap context for MethodType objects.
     */
    protected final Context defaultHctx;

    protected final Map<Var, Set<Invoke>> relevantVars = MapUtils.newHybridMap();

    protected final Map<JMethod, Consumer<Invoke>> varRegisters
            = MapUtils.newHybridMap();

    protected final Map<JMethod, TriConsumer<CSVar, PointsToSet, Invoke>> handlers
            = MapUtils.newHybridMap();

    protected AbstractModel(Solver solver) {
        this.solver = solver;
        hierarchy = solver.getHierarchy();
        csManager = solver.getCSManager();
        heapModel = solver.getHeapModel();
        defaultHctx = solver.getContextSelector().getDefaultContext();
    }

    public boolean isRelevantVar(Var var) {
        return relevantVars.containsKey(var);
    }

    protected void addRelevantBase(Invoke invoke) {
        InvokeInstanceExp ie = (InvokeInstanceExp) invoke.getInvokeExp();
        MapUtils.addToMapSet(relevantVars, ie.getBase(), invoke);
    }

    protected void addRelevantArg(Invoke invoke, int i) {
        MapUtils.addToMapSet(relevantVars,
                invoke.getInvokeExp().getArg(i), invoke);
    }

    protected void addVarRegister(JMethod api, Consumer<Invoke> register) {
        varRegisters.put(api, register);
    }

    /**
     * Matches an invocation and API by their declaring class and method name.
     * TODO: resolve invoke's method reference?
     */
    protected boolean matchAPI(Invoke invoke, JMethod api) {
        MethodRef ref = invoke.getMethodRef();
        return ref.getDeclaringClass().equals(api.getDeclaringClass()) &&
                ref.getName().equals(api.getName());
    }

    @Override
    public void handleNewInvoke(Invoke invoke) {
        varRegisters.forEach((api, action) -> {
            if (matchAPI(invoke, api)) {
                action.accept(invoke);
            }
        });
    }

    protected void addAPIHandler(
            JMethod api, TriConsumer<CSVar, PointsToSet, Invoke> handler) {
        handlers.put(api, handler);
    }

    @Override
    public void handleNewPointsToSet(CSVar csVar, PointsToSet pts) {
        relevantVars.get(csVar.getVar()).forEach(invoke ->
                handlers.forEach((api, handler) -> {
                    if (matchAPI(invoke, api)) {
                        handler.accept(csVar, pts, invoke);
                    }
                })
        );
    }

    /**
     * For invocation r = v.foo(a0, ...);
     * when points-to set of v or a0 changes,
     * this convenient method returns points-to sets of v and a0.
     * For variable csVar.getVar(), this method returns pts,
     * otherwise, it just returns current points-to set of the variable.
     * @param csVar may be v or a0.
     * @param pts changed part of csVar
     * @param ie the call site which contain csVar
     */
    protected List<PointsToSet> getBaseArg0(
            CSVar csVar, PointsToSet pts, InvokeInstanceExp ie) {
        PointsToSet basePts, arg0Pts;
        if (csVar.getVar().equals(ie.getBase())) {
            basePts = pts;
            arg0Pts = solver.getPointsToSetOf(
                    csManager.getCSVar(csVar.getContext(), ie.getArg(0)));
        } else {
            basePts = solver.getPointsToSetOf(
                    csManager.getCSVar(csVar.getContext(), ie.getBase()));
            arg0Pts = pts;
        }
        return List.of(basePts, arg0Pts);
    }

    /**
     * For invocation r = foo(a0, a1, ...);
     * when points-to set of a0 or a1 changes,
     * this convenient method returns points-to sets of a0 and a1.
     * For variable csVar.getVar(), this method returns pts,
     * otherwise, it just returns current points-to set of the variable.
     * @param csVar may be a0 or a1.
     * @param pts changed part of csVar
     * @param ie the call site which contain csVar
     */
    protected List<PointsToSet> getArg01(
            CSVar csVar, PointsToSet pts, InvokeExp ie) {
        PointsToSet arg0Pts, arg1Pts;
        if (csVar.getVar().equals(ie.getArg(0))) {
            arg0Pts = pts;
            arg1Pts = solver.getPointsToSetOf(
                    csManager.getCSVar(csVar.getContext(), ie.getArg(1)));
        } else {
            arg0Pts = solver.getPointsToSetOf(
                    csManager.getCSVar(csVar.getContext(), ie.getArg(0)));
            arg1Pts = pts;
        }
        return List.of(arg0Pts, arg1Pts);
    }
}
