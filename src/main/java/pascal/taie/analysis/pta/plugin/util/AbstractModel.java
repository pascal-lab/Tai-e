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
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    protected final Map<JMethod, int[]> relevantVarIndexes = Maps.newHybridMap();

    protected final MultiMap<Var, Invoke> relevantVars
            = Maps.newMultiMap(Maps.newHybridMap());

    protected final Map<JMethod, TriConsumer<CSVar, PointsToSet, Invoke>> handlers
            = Maps.newHybridMap();

    protected AbstractModel(Solver solver) {
        this.solver = solver;
        hierarchy = solver.getHierarchy();
        csManager = solver.getCSManager();
        heapModel = solver.getHeapModel();
        defaultHctx = solver.getContextSelector().getEmptyContext();
        registerVarAndHandler();
    }

    protected abstract void registerVarAndHandler();

    protected void registerRelevantVarIndexes(JMethod api, int... indexes) {
        relevantVarIndexes.put(api, indexes);
    }

    @Override
    public void handleNewInvoke(Invoke invoke) {
        JMethod target = invoke.getMethodRef().resolveNullable();
        if (target != null) {
            int[] indexes = relevantVarIndexes.get(target);
            if (indexes != null) {
                for (int i : indexes) {
                    relevantVars.put(getArg(invoke, i), invoke);
                }
            }
        }
    }

    @Override
    public boolean isRelevantVar(Var var) {
        return relevantVars.containsKey(var);
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
     * For invocation r = v.foo(a0, a1, ..., an);
     * when points-to set of v or any ai (0 <= i <= n) changes,
     * this convenient method returns points-to sets relevant arguments.
     * For case v/ai == csVar.getVar(), this method returns pts,
     * otherwise, it just returns current points-to set of v/ai.
     *
     * @param csVar   may be v or any ai.
     * @param pts     changed part of csVar
     * @param invoke  the call site which contain csVar
     * @param indexes indexes of the relevant arguments
     */
    protected List<PointsToSet> getArgs(
            CSVar csVar, PointsToSet pts, Invoke invoke, int... indexes) {
        List<PointsToSet> args = new ArrayList<>(indexes.length);
        for (int i : indexes) {
            Var arg = getArg(invoke, i);
            if (arg.equals(csVar.getVar())) {
                args.add(pts);
            } else {
                CSVar csArg = csManager.getCSVar(csVar.getContext(), arg);
                args.add(solver.getPointsToSetOf(csArg));
            }
        }
        return args;
    }

    private static Var getArg(Invoke invoke, int i) {
        InvokeExp invokeExp = invoke.getInvokeExp();
        return i == BASE ?
                ((InvokeInstanceExp) invokeExp).getBase() :
                invokeExp.getArg(i);
    }
}
