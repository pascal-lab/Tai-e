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

package pascal.taie.analysis.pta.plugin;

import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JMethod;

/**
 * Analysis plugin interface.
 * <p>
 * This interface contains callbacks for pointer analysis events.
 * It is supposed to provide a mechanism for extending functionalities
 * of the analysis, so its implementations may have side effects
 * on pointer analysis.
 */
public interface Plugin {

    Plugin DUMMY = new Plugin() {};

    /**
     * Sets pointer analysis solver which will be used later by the plugin.
     */
    default void setSolver(Solver solver) {
    }

    /**
     * Invoked when pointer analysis starts.
     */
    default void onStart() {
    }

    /**
     * Invoked when pointer analysis has processed all entries in the work list.
     * Some plugins need to perform certain computation at this stage
     * (so that it can collect enough points-to information in the program),
     * and may further add entries to the work list to "restart" the
     * pointer analysis.
     */
    default void onPhaseFinish() {
    }

    /**
     * Invoked when pointer analysis finishes.
     * Pointer analysis is supposed to have been finished at this stage,
     * thus this call back should NOT modify pointer analysis results.
     */
    default void onFinish() {
    }

    /**
     * Invoked when set of new objects flow to a context-sensitive variable.
     *
     * @param csVar variable whose points-to set changes
     * @param pts   set of new objects
     */
    default void onNewPointsToSet(CSVar csVar, PointsToSet pts) {
    }

    /**
     * Invoked when a new call graph edge is discovered.
     *
     * @param edge new call graph edge
     */
    default void onNewCallEdge(Edge<CSCallSite, CSMethod> edge) {
    }

    /**
     * Invoked when a new reachable method is discovered.
     *
     * @param method new reachable method
     */
    default void onNewMethod(JMethod method) {
    }

    /**
     * Invoked when a new reachable stmt is discovered.
     *
     * @param stmt      new reachable stmt
     * @param container container method of {@code stmt}
     */
    default void onNewStmt(Stmt stmt, JMethod container) {
    }

    /**
     * Invoked when a new reachable context-sensitive method is discovered.
     *
     * @param csMethod new reachable context-sensitive method
     */
    default void onNewCSMethod(CSMethod csMethod) {
    }

    /**
     * Invoked when pointer analysis failed to resolve callee (i.e., resolve
     * to null) on a receiver object. Some plugins take over such cases to
     * do their analyses.
     *
     * @param recv    the receiver object
     * @param context the context of the invocation
     * @param invoke  the invocation site
     */
    default void onUnresolvedCall(CSObj recv, Context context, Invoke invoke) {
    }
}
