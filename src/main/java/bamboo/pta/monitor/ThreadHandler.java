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

import bamboo.pta.core.ProgramManager;
import bamboo.pta.core.context.Context;
import bamboo.pta.core.cs.CSMethod;
import bamboo.pta.core.cs.CSObj;
import bamboo.pta.core.cs.CSVariable;
import bamboo.pta.core.solver.PointerAnalysis;
import bamboo.pta.element.Method;
import bamboo.pta.element.Obj;
import bamboo.pta.element.Variable;
import bamboo.pta.env.Environment;
import bamboo.pta.set.PointsToSet;

import java.util.Set;

/**
 * Model initialization of main thread, system thread group,
 * and some Thread APIs.
 */
public class ThreadHandler implements AnalysisMonitor {

    private static final String START = "<java.lang.Thread: void start()>";

    private PointerAnalysis pta;
    private ProgramManager pm;

    /**
     * Set of running threads.
     */
    private Set<CSObj> runningThreads;
    /**
     * This variable of Thread.start().
     */
    private Variable threadStartThis;
    /**
     * Context-sensitive return variable of Thread.currentThread().
     */
    private Set<CSVariable> currentThreadReturns;

    @Override
    public void setPointerAnalysis(PointerAnalysis pta) {
        this.pta = pta;
        this.pm = pta.getProgramManager();
    }

    @Override
    public void signalInitialization() {
        Environment env = pm.getEnvironment();
        Context context = pta.getContextSelector().getDefaultContext();

        // setup system thread group
        // propagate <system-thread-group> to <java.lang.ThreadGroup: void <init>()>/this
        Obj systemThreadGroup = env.getSystemThreadGroup();
        Method threadGroupInit = pm.getUniqueMethodBySignature(
                "<java.lang.ThreadGroup: void <init>()>");
        Variable initThis = threadGroupInit.getThis();
        pta.addPointsTo(context, initThis, context, systemThreadGroup);

        // setup main thread group
        // propagate <main-thread-group> to <java.lang.ThreadGroup: void
        //   <init>(java.lang.ThreadGroup,java.lang.String)>/this
        Obj mainThreadGroup = env.getMainThreadGroup();
        threadGroupInit = pm.getUniqueMethodBySignature(
                "<java.lang.ThreadGroup: void <init>(java.lang.ThreadGroup,java.lang.String)>");
        initThis = threadGroupInit.getThis();
        pta.addPointsTo(context, initThis, context, mainThreadGroup);
        // propagate <system-thread-group> to param0
        Variable param0 = threadGroupInit.getParam(0).get();
        pta.addPointsTo(context, param0, context, systemThreadGroup);
        // propagate "main" to param1
        Variable param1 = threadGroupInit.getParam(1).get();
        Obj main = env.getStringConstant("main");
        pta.addPointsTo(context, param1, context, main);

        // setup main thread
        // propagate <main-thread> to <java.lang.Thread: void
        //   <init>(java.lang.ThreadGroup,java.lang.String)>/this
        Obj mainThread = env.getMainThread();
        Method threadInit = pm.getUniqueMethodBySignature(
                "<java.lang.Thread: void <init>(java.lang.ThreadGroup,java.lang.String)>");
        initThis = threadInit.getThis();
        pta.addPointsTo(context, initThis, context, mainThread);
        // propagate <main-thread-group> to param0
        param0 = threadInit.getParam(0).get();
        pta.addPointsTo(context, param0, context, mainThreadGroup);
        // propagate "main" to param1
        param1 = threadInit.getParam(1).get();
        pta.addPointsTo(context, param1, context, main);
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

    @Override
    public void signalNewCSMethod(CSMethod csMethod) {
        // add return variable os Thread.currentThread()
    }
}
