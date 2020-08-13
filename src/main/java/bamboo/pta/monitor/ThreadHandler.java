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
import bamboo.pta.core.context.ContextSelector;
import bamboo.pta.core.cs.CSManager;
import bamboo.pta.core.cs.CSMethod;
import bamboo.pta.core.cs.CSObj;
import bamboo.pta.core.cs.CSVariable;
import bamboo.pta.core.solver.PointerAnalysis;
import bamboo.pta.element.Method;
import bamboo.pta.element.Obj;
import bamboo.pta.element.Variable;
import bamboo.pta.env.Environment;
import bamboo.pta.set.PointsToSet;
import bamboo.pta.set.PointsToSetFactory;

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
        CSManager csManager = pta.getCSManager();
        PointsToSetFactory setFactory = pta.getPointsToSetFactory();
        Context context = pta.getContextSelector().getDefaultContext();

        // setup system thread group
        // propagate <system-thread-group> to <java.lang.ThreadGroup: void <init>()>/this
        Obj systemThreadGroup = env.getSystemThreadGroup();
        CSObj csSystemThreadGroup = csManager.getCSObj(context, systemThreadGroup);
        Method threadGroupInit = pm.getUniqueMethodBySignature(
                "<java.lang.ThreadGroup: void <init>()>");
        Variable initThis = threadGroupInit.getThis();
        CSVariable csInitThis = csManager.getCSVariable(context, initThis);
        pta.addPointerEntry(csInitThis,
                setFactory.makePointsToSet(csSystemThreadGroup));

        // setup main thread group
        // propagate <main-thread-group> to <java.lang.ThreadGroup: void
        //   <init>(java.lang.ThreadGroup,java.lang.String)>/this
        Obj mainThreadGroup = env.getMainThreadGroup();
        CSObj csMainThreadGroup = csManager.getCSObj(context, mainThreadGroup);
        threadGroupInit = pm.getUniqueMethodBySignature(
                "<java.lang.ThreadGroup: void <init>(java.lang.ThreadGroup,java.lang.String)>");
        initThis = threadGroupInit.getThis();
        csInitThis = csManager.getCSVariable(context, initThis);
        pta.addPointerEntry(csInitThis,
                setFactory.makePointsToSet(csMainThreadGroup));
        // propagate <system-thread-group> to param0
        Variable param0 = threadGroupInit.getParam(0).get();
        CSVariable csParam0 = csManager.getCSVariable(context, param0);
        pta.addPointerEntry(csParam0,
                setFactory.makePointsToSet(csSystemThreadGroup));
        // propagate "main" to param1
        Variable param1 = threadGroupInit.getParam(1).get();
        CSVariable csParam1 = csManager.getCSVariable(context, param1);
        Obj main = env.getStringConstant("main");
        CSObj csMain = csManager.getCSObj(context, main);
        pta.addPointerEntry(csParam1, setFactory.makePointsToSet(csMain));

        // setup main thread
        // propagate <main-thread> to <java.lang.Thread: void
        //   <init>(java.lang.ThreadGroup,java.lang.String)>/this
        Obj mainThread = env.getMainThread();
        CSObj csMainThread = csManager.getCSObj(context, mainThread);
        Method threadInit = pm.getUniqueMethodBySignature(
                "<java.lang.Thread: void <init>(java.lang.ThreadGroup,java.lang.String)>");
        initThis = threadInit.getThis();
        csInitThis = csManager.getCSVariable(context, initThis);
        pta.addPointerEntry(csInitThis, setFactory.makePointsToSet(csMainThread));
        // propagate <main-thread-group> to param0
        param0 = threadInit.getParam(0).get();
        csParam0 = csManager.getCSVariable(context, param0);
        pta.addPointerEntry(csParam0,
                setFactory.makePointsToSet(csMainThreadGroup));
        // propagate "main" to param1
        param1 = threadInit.getParam(1).get();
        csParam1 = csManager.getCSVariable(context, param1);
        pta.addPointerEntry(csParam1, setFactory.makePointsToSet(csMain));
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
