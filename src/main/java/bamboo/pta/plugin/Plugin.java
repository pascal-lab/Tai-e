/*
 * Bamboo - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Bamboo is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package bamboo.pta.plugin;

import bamboo.pta.core.cs.CSMethod;
import bamboo.pta.core.cs.CSVariable;
import bamboo.pta.core.solver.PointerAnalysis;
import bamboo.pta.element.Method;
import bamboo.pta.set.PointsToSet;

/**
 * Analysis plugin interface.
 * This interface contains callbacks for pointer analysis events.
 * It is suppose to provide a mechanism for extending functionalities
 * of the analysis, so its implementations would have side effects
 * on pointer analysis and should be thread-safe.
 */
public interface Plugin {

    /**
     * Set pointer analysis interface which will be used later by the monitor.
     */
    default void setPointerAnalysis(PointerAnalysis pta) {
    }

    /**
     * Invoked during pre-processing, i.e., before pointer analysis starts.
     * Thread-safe.
     */
    default void preprocess() {
    }

    /**
     * Invoked during pointer analysis initialization.
     * Thread-safe.
     */
    default void initialize() {
    }

    /**
     * Invoked after pointer analysis finishes.
     * Thread-safe.
     */
    default void finish() {
    }

    /**
     * Invoked when set of new objects flow to a context-sensitive variable.
     * Not thread-safe, but single-thread on csVar.
     * @param csVar variable whose points-to set changes
     * @param pts set of new objects
     */
    default void handleNewPointsToSet(CSVariable csVar, PointsToSet pts) {
    }

    /**
     * Invoked when new reachable method is discovered.
     * Not thread-safe, but single-thread on method.
     * @param method new reachable method
     */
    default void handleNewMethod(Method method) {
    }

    /**
     * Invoked when new reachable context-sensitive method is discovered.
     * Not thread-safe, but single-thread on csMethod.
     * @param csMethod new reachable context-sensitive method
     */
    default void handleNewCSMethod(CSMethod csMethod) {
    }
}
