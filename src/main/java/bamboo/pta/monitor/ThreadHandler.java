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

package bamboo.pta.monitor;

import bamboo.pta.core.cs.CSObj;
import bamboo.pta.core.cs.CSVariable;
import bamboo.pta.core.solver.PointerAnalysis;
import bamboo.pta.element.Method;
import bamboo.pta.element.Variable;
import bamboo.pta.set.PointsToSet;

import java.util.Set;

/**
 * Model initialization of main thread, system thread group,
 * and some Thread APIs.
 */
public class ThreadHandler implements AnalysisMonitor {

    private static final String START = "<java.lang.Thread: void start()>";

    /**
     * Set of running threads.
     */
    private Set<CSObj> runningThreads;
    /**
     * This variable of Thread.start();
     */
    private Variable threadStartThis;
    /**
     * Context-sensitive return variable of Thread.currentThread();
     */
    private Set<CSVariable> currentThreadReturns;

    @Override
    public void setPointerAnalysis(PointerAnalysis pta) {
    }

    @Override
    public void signalInitialization() {
        // setup system thread group
        // setup main thread group
        // setup main thread
    }

    @Override
    public void signalNewPointsToSet(CSVariable csVar, PointsToSet pts) {
        // propagate thread objects to return value of Thread.currentThread()
    }

    @Override
    public void signalNewMethod(Method method) {
        if (method.getSignature().equals(START)) {
            threadStartThis = method.getThis();
        }
    }
}
