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

package pascal.taie.analysis.pta.plugin.exception;

import pascal.taie.analysis.exception.CatchAnalysis;
import pascal.taie.analysis.graph.callgraph.CallKind;
import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSManager;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.Plugin;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.proginfo.ExceptionEntry;
import pascal.taie.ir.stmt.Catch;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.ir.stmt.Throw;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.TypeSystem;
import pascal.taie.util.collection.IndexerBitSet;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.SetEx;
import pascal.taie.util.collection.Sets;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.function.Supplier;

public class ExceptionAnalysis implements Plugin {

    private Solver solver;

    private CSManager csManager;

    private TypeSystem typeSystem;

    private Supplier<SetEx<CSObj>> setFactory;

    /**
     * Map from thrown variables to the corresponding throw statements.
     */
    private MultiMap<Var, Throw> var2Throws = Maps.newMultiMap();

    /**
     * Map from each method to the result of catch analysis on it.
     */
    private Map<JMethod, Map<Stmt, List<ExceptionEntry>>> catchers =
            Maps.newMap(1024);

    /**
     * Work-list of exception entries to be propagated.
     */
    private Queue<Entry> workList = new ArrayDeque<>();

    /**
     * Work-list entries.
     */
    private record Entry(CSMethod csMethod, Stmt stmt, Set<CSObj> exceptions) {
    }

    @Override
    public void setSolver(Solver solver) {
        this.solver = solver;
        this.csManager = solver.getCSManager();
        this.typeSystem = solver.getTypeSystem();
        // use sparse bit set only when exception objects are NOT merged
        boolean isSparse = !solver.getOptions().getBoolean("merge-exception-objects");
        this.setFactory = () -> new IndexerBitSet<>(csManager.getObjectIndexer(), isSparse);
    }

    /**
     * For the throw statements in the method, analyzes and records all the
     * exception entries that handle the exceptions thrown by the statements.
     *
     * @param method the method that the solver meet now
     */
    @Override
    public void onNewMethod(JMethod method) {
        catchers.put(method,
                CatchAnalysis.getPotentialCatchers(method.getIR()));
    }

    /**
     * Establishes the map from all exception references to related throw statements.
     *
     * @param stmt new reachable stmt
     */
    @Override
    public void onNewStmt(Stmt stmt, JMethod container) {
        if (stmt instanceof Throw throwStmt) {
            Var exceptionRef = throwStmt.getExceptionRef();
            var2Throws.put(exceptionRef, throwStmt);
        }
    }

    /**
     * If the csVar is an exception reference, propagate all the exception
     * it newly throws.
     *
     * @param csVar variable pointer
     * @param pts   objects added to the csVar points to set
     */
    @Override
    public void onNewPointsToSet(CSVar csVar, PointsToSet pts) {
        Set<Throw> throwStmts = var2Throws.get(csVar.getVar());
        if (!throwStmts.isEmpty()) {
            Var exceptionRef = csVar.getVar();
            Context ctx = csVar.getContext();
            JMethod currentMethod = exceptionRef.getMethod();
            CSMethod currentCSMethod = csManager.getCSMethod(ctx, currentMethod);
            throwStmts.forEach(throwStmt -> workList.add(
                    new Entry(currentCSMethod, throwStmt, pts.getObjects())));
            propagateExceptions();
        }
    }

    /**
     * For a new call edge, the exception thrown by the callee method should be
     * propagated to its callers, and thrown by the invoke statement,
     * then we propagate the thrown exceptions accordingly.
     *
     * @param edge the newly established call edge
     */
    @Override
    public void onNewCallEdge(Edge<CSCallSite, CSMethod> edge) {
        if (edge.getKind() != CallKind.OTHER) {
            // currently, don't propagate exceptions along OTHER edges
            CSMethod callee = edge.getCallee();
            Optional<CSMethodThrowResult> csResult =
                    callee.getResult(getClass().getName());
            csResult.ifPresent(result -> {
                CSMethod caller = edge.getCallSite().getContainer();
                Invoke invoke = edge.getCallSite().getCallSite();
                Set<CSObj> exceptions = result.mayThrowUncaught();
                workList.add(new Entry(caller, invoke, exceptions));
                propagateExceptions();
            });
        }
    }

    /**
     * Propagates exceptions from callees to callers (and callers' caller).
     * When a statement throws new exceptions, call {@link #analyzeIntraUncaught}
     * to handle the exceptions that can be caught by the containing method.
     * If there are uncaught exceptions in the method, they will be propagated
     * to call site (invoke) of the method.
     */
    private void propagateExceptions() {
        while (!workList.isEmpty()) {
            Entry entry = workList.poll();
            CSMethod csMethod = entry.csMethod();
            Stmt stmt = entry.stmt();
            Set<CSObj> exceptions = entry.exceptions();
            CSMethodThrowResult result = csMethod.getResult(getClass().getName(),
                    () -> new CSMethodThrowResult(setFactory));
            Set<CSObj> diff = result.propagate(stmt, exceptions);
            if (!diff.isEmpty()) {
                Set<CSObj> uncaught = analyzeIntraUncaught(
                        stmt, diff, csMethod);
                if (!uncaught.isEmpty()) {
                    result.addUncaughtExceptions(uncaught);
                    solver.getCallGraph()
                            .edgesInTo(csMethod)
                            // currently, don't propagate exceptions along OTHER edges
                            .filter(edge -> edge.getKind() != CallKind.OTHER)
                            .forEach(edge -> {
                                CSCallSite callSite = edge.getCallSite();
                                CSMethod caller = callSite.getContainer();
                                Invoke invoke = callSite.getCallSite();
                                workList.add(new Entry(caller, invoke, uncaught));
                            });
                }
            }
        }
    }

    /**
     * Performs an intra-procedural analysis to compute the exceptions that are
     * not caught by the current method.
     *
     * @param currentStmt   the statements that throws exceptions
     * @param newExceptions the new-found exceptions thrown by currentStmt
     * @param csMethod      the csMethod containing currentStmt
     * @return the exceptions thrown by currentStmt but not caught by csMethod
     */
    private Set<CSObj> analyzeIntraUncaught(
            Stmt currentStmt,
            Set<CSObj> newExceptions,
            CSMethod csMethod) {
        List<ExceptionEntry> entries = catchers.get(csMethod.getMethod())
                .get(currentStmt);
        if (entries != null) {
            Context ctx = csMethod.getContext();
            for (ExceptionEntry entry : entries) {
                Set<CSObj> uncaughtExceptions = Sets.newHybridSet();
                newExceptions.forEach(newException -> {
                    Obj exObj = newException.getObject();
                    if (typeSystem.isSubtype(entry.catchType(), exObj.getType())) {
                        Catch catchStmt = entry.handler();
                        Var exceptionRef = catchStmt.getExceptionRef();
                        solver.addVarPointsTo(ctx, exceptionRef, newException);
                    } else {
                        uncaughtExceptions.add(newException);
                    }
                });
                newExceptions = uncaughtExceptions;
            }
        }
        return newExceptions;
    }

    @Override
    public void onFinish() {
        // Collects context-sensitive throw results and stores them in
        // a context-insensitive manner.
        PTAThrowResult throwResult = new PTAThrowResult();
        for (CSMethod csMethod : solver.getCallGraph()) {
            JMethod method = csMethod.getMethod();
            MethodThrowResult result = throwResult.getOrCreateResult(method);
            Optional<CSMethodThrowResult> csResult =
                    csMethod.getResult(getClass().getName());
            csResult.ifPresent(result::addCSMethodThrowResult);
        }
        solver.getResult().storeResult(getClass().getName(), throwResult);
        clear();
    }

    private void clear() {
        var2Throws = null;
        catchers = null;
        workList = null;
    }
}
