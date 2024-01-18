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

package pascal.taie.analysis.pta.plugin.util;

import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.AnalysisException;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Provides common functionalities for implementing the plugins which
 * model the APIs by analyzing their semantics.
 * The invoke handler method (annotated by {@link InvokeHandler})
 * should follow such declaration:
 * public void name(Context,Invoke,(PointsToSet,)+)
 *
 * @see InvokeHandler
 */
public abstract class AnalysisModelPlugin extends ModelPlugin {

    protected final Map<JMethod, Method> handlers = Maps.newMap();

    protected final Map<JMethod, int[]> relevantVarIndexes = Maps.newMap();

    protected final MultiMap<Var, Invoke> relevantVars = Maps.newMultiMap();

    protected AnalysisModelPlugin(Solver solver) {
        super(solver);
        registerHandlers();
    }

    @Override
    protected void registerHandler(InvokeHandler invokeHandler, Method handler) {
        for (String signature : invokeHandler.signature()) {
            JMethod api = hierarchy.getMethod(signature);
            if (api != null) {
                if (handlers.containsKey(api)) {
                    throw new RuntimeException(this + " registers multiple handlers for " +
                            api + " (in a Model, at most one handler can be registered for a method)");
                }
                handlers.put(api, validate(handler, invokeHandler));
                relevantVarIndexes.put(api, invokeHandler.argIndexes());
            }
        }
    }

    /**
     * Validates the declaration of invoke handler.
     */
    private static Method validate(Method handler, InvokeHandler invokeHandler) {
        // check handler parameter type
        int nArgs = invokeHandler.argIndexes().length;
        Class<?>[] paramTypes = handler.getParameterTypes();
        if (paramTypes.length == 2 + nArgs
                && paramTypes[0] == Context.class
                && paramTypes[1] == Invoke.class
                && IntStream.range(2, paramTypes.length)
                .allMatch(i -> paramTypes[i] == PointsToSet.class)) {
            return handler;
        }
        throw new RuntimeException("Illegal handler declaration of " + invokeHandler +
                "\nexpected: (Context,Invoke" + ",PointsToSet".repeat(nArgs) + ")" +
                "\ngiven: " + handler);
    }

    @Override
    public void onNewStmt(Stmt stmt, JMethod container) {
        if (stmt instanceof Invoke invoke && !invoke.isDynamic()) {
            JMethod target = invoke.getMethodRef().resolveNullable();
            if (target != null) {
                int[] indexes = relevantVarIndexes.get(target);
                if (indexes != null) {
                    for (int i : indexes) {
                        relevantVars.put(InvokeUtils.getVar(invoke, i), invoke);
                    }
                }
            }
        }
    }

    @Override
    public void onNewPointsToSet(CSVar csVar, PointsToSet pts) {
        relevantVars.get(csVar.getVar()).forEach(invoke -> {
            JMethod target = invoke.getMethodRef().resolve();
            Method handler = handlers.get(target);
            if (handler != null) {
                int[] indexes = relevantVarIndexes.get(target);
                invokeHandler(handler, invoke, csVar, pts, indexes);
            }
        });
    }

    private void invokeHandler(Method handler, Invoke invoke,
                               CSVar csVar, PointsToSet pts, int[] indexes) {
        PointsToSet[] args = getArgs(csVar, pts, invoke, indexes);
        Object[] invokeArgs = new Object[2 + args.length];
        invokeArgs[0] = csVar.getContext();
        invokeArgs[1] = invoke;
        System.arraycopy(args, 0, invokeArgs, 2, args.length);
        try {
            handler.invoke(this, invokeArgs);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new AnalysisException(e);
        }
    }

    /**
     * For invocation r = v.foo(a0, a1, ..., an);
     * when points-to set of v or any ai (0 &le; i &le; n) changes,
     * this convenient method returns points-to sets relevant arguments.
     * For case v/ai == csVar.getVar(), this method returns pts,
     * otherwise, it just returns current points-to set of v/ai.
     *
     * @param csVar   may be v or any ai.
     * @param pts     changed part of csVar
     * @param invoke  the call site which contain csVar
     * @param indexes indexes of the relevant arguments
     */
    private PointsToSet[] getArgs(
            CSVar csVar, PointsToSet pts, Invoke invoke, int[] indexes) {
        PointsToSet[] args = new PointsToSet[indexes.length];
        for (int i = 0; i < args.length; ++i) {
            int index = indexes[i];
            Var arg = InvokeUtils.getVar(invoke, index);
            if (arg.equals(csVar.getVar())) {
                args[i] = pts;
            } else {
                CSVar csArg = csManager.getCSVar(csVar.getContext(), arg);
                args[i] = solver.getPointsToSetOf(csArg);
            }
        }
        return args;
    }
}
