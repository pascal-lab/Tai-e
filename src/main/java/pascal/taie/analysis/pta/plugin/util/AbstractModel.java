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
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.TriConsumer;
import pascal.taie.util.collection.MapUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Provides common functionalities for implementing API models.
 */
public abstract class AbstractModel implements Model {

    /**
     * Special index representing the base variable of an invocation site.
     */
    protected static final int BASE = -1;

    protected final Solver solver;

    protected final ClassHierarchy hierarchy;

    protected final CSManager csManager;

    protected final HeapModel heapModel;

    /**
     * Default heap context for MethodType objects.
     */
    protected final Context defaultHctx;

    protected final Map<JMethod, int[]> relevantVarIndexes = MapUtils.newHybridMap();

    protected final Map<Var, Set<Invoke>> relevantVars = MapUtils.newHybridMap();

    protected final Map<JMethod, TriConsumer<CSVar, PointsToSet, Invoke>> handlers
            = MapUtils.newHybridMap();

    protected AbstractModel(Solver solver) {
        this.solver = solver;
        hierarchy = solver.getHierarchy();
        csManager = solver.getCSManager();
        heapModel = solver.getHeapModel();
        defaultHctx = solver.getContextSelector().getDefaultContext();
        initialize();
    }

    protected void initialize() {
    }

    protected void addRelevantBase(Invoke invoke) {
        InvokeInstanceExp ie = (InvokeInstanceExp) invoke.getInvokeExp();
        MapUtils.addToMapSet(relevantVars, ie.getBase(), invoke);
    }

    protected void addRelevantArg(Invoke invoke, int i) {
        MapUtils.addToMapSet(relevantVars,
                invoke.getInvokeExp().getArg(i), invoke);
    }

    protected void registerRelevantVarIndexes(JMethod api, int... indexes) {
        relevantVarIndexes.put(api, indexes);
    }

    public boolean isRelevantVar(Var var) {
        return relevantVars.containsKey(var);
    }

    @Override
    public void handleNewInvoke(Invoke invoke) {
        JMethod target = invoke.getMethodRef().resolve();
        int[] indexes = relevantVarIndexes.get(target);
        if (indexes != null) {
            InvokeExp invokeExp = invoke.getInvokeExp();
            for (int i : indexes) {
                Var var = i == BASE ? getBase(invoke) : invokeExp.getArg(i);
                MapUtils.addToMapSet(relevantVars, var, invoke);
            }
        }
    }

    protected void registerAPIHandler(
            JMethod api, TriConsumer<CSVar, PointsToSet, Invoke> handler) {
        handlers.put(api, handler);
    }

    @Override
    public void handleNewPointsToSet(CSVar csVar, PointsToSet pts) {
        relevantVars.get(csVar.getVar()).forEach(invoke -> {
            JMethod target = invoke.getMethodRef().resolve();
            var handler = handlers.get(target);
            if (handler != null) {
                handler.accept(csVar, pts, invoke);
            }
        });
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
     * For invocation r = v.foo(a0, a1, ..., an);
     * when points-to set of v or any ai (0 <= i <= n) changes,
     * this convenient method returns points-to sets relevant arguments.
     * For case v/ai == csVar.getVar(), this method returns pts,
     * otherwise, it just returns current points-to set of v/ai.
     * @param csVar may be v or any ai.
     * @param pts changed part of csVar
     * @param invoke the call site which contain csVar
     * @param indexes indexes of the relevant arguments
     */
    protected List<PointsToSet> getArgs(
            CSVar csVar, PointsToSet pts, Invoke invoke, int... indexes) {
        List<PointsToSet> args = new ArrayList<>(indexes.length);
        InvokeExp invokeExp = invoke.getInvokeExp();
        for (int i : indexes) {
            Var arg = i == BASE ? getBase(invoke) : invokeExp.getArg(i);
            if (arg.equals(csVar.getVar())) {
                args.add(pts);
            } else {
                CSVar csArg = csManager.getCSVar(csVar.getContext(), arg);
                args.add(solver.getPointsToSetOf(csArg));
            }
        }
        return args;
    }

    private static Var getBase(Invoke invoke) {
        return ((InvokeInstanceExp) invoke.getInvokeExp()).getBase();
    }
}
