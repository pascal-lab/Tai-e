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

package pascal.taie.analysis.pta.plugin;

import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.language.classes.JMethod;

/**
 * Analysis plugin interface.
 * This interface contains callbacks for pointer analysis events.
 * It is suppose to provide a mechanism for extending functionalities
 * of the analysis, so its implementations would have side effects
 * on pointer analysis and should be thread-safe.
 */
public interface Plugin {

    /**
     * Set pointer analysis solver which will be used later by the plugin.
     */
    default void setSolver(Solver solver) {
    }

    /**
     * Invoked during pre-processing, i.e., before pointer analysis starts.
     * Thread-safe.
     */
    default void preprocess() {
    }

    /**
     * Invoked when pointer analysis initializes.
     * Thread-safe.
     */
    default void initialize() {
    }

    /**
     * Invoked when pointer analysis finishes.
     * Thread-safe.
     */
    default void finish() {
    }

    /**
     * Invoked during post-processing, i.e., after pointer analysis finishes.
     * Thread-safe.
     */
    default void postprocess() {
    }

    /**
     * Invoked when set of new objects flow to a context-sensitive variable.
     * Not thread-safe, but single-thread on csVar.
     * @param csVar variable whose points-to set changes
     * @param pts set of new objects
     */
    default void handleNewPointsToSet(CSVar csVar, PointsToSet pts) {
    }

    /**
     * Invoked when a new call graph edge is discovered.
     * Not thread-safe, but single-thread on edge.
     * @param edge new call graph edge
     */
    default void handleNewCallEdge(Edge<CSCallSite, CSMethod> edge) {
    }

    /**
     * Invoked when a new reachable method is discovered.
     * Not thread-safe, but single-thread on method.
     * @param method new reachable method
     */
    default void handleNewMethod(JMethod method) {
    }

    /**
     * Invoked when a new reachable context-sensitive method is discovered.
     * Not thread-safe, but single-thread on csMethod.
     * @param csMethod new reachable context-sensitive method
     */
    default void handleNewCSMethod(CSMethod csMethod) {
    }
}
