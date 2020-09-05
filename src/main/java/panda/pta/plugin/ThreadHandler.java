/*
 * Panda - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Panda is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package panda.pta.plugin;

import panda.pta.core.ProgramManager;
import panda.pta.core.context.Context;
import panda.pta.core.cs.CSMethod;
import panda.pta.core.cs.CSVariable;
import panda.pta.core.solver.PointerAnalysis;
import panda.pta.element.Method;
import panda.pta.element.Obj;
import panda.pta.element.Variable;
import panda.pta.env.Environment;
import panda.pta.options.Options;
import panda.pta.set.PointsToSet;
import panda.pta.set.PointsToSetFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * Model initialization of main thread, system thread group,
 * and some Thread APIs.
 */
public class ThreadHandler implements Plugin {

    private PointerAnalysis pta;
    private ProgramManager pm;

    /**
     * This variable of Thread.start().
     */
    private Variable threadStartThis;
    /**
     * Set of running threads.
     */
    private final PointsToSet runningThreads = PointsToSetFactory.make();
    /**
     * Represent Thread.currentThread.
     */
    private Method currentThread;
    /**
     * Return variable of Thread.currentThread().
     */
    private Variable currentThreadReturn;
    /**
     * Contexts of Thread.currentThread().
     */
    private final Set<Context> currentThreadContexts = new HashSet<>();

    @Override
    public void setPointerAnalysis(PointerAnalysis pta) {
        this.pta = pta;
        pm = pta.getProgramManager();
        threadStartThis = pm.getUniqueMethodBySignature(
                "<java.lang.Thread: void start()>").getThis();
        currentThread = pm.getUniqueMethodBySignature(
                "<java.lang.Thread: java.lang.Thread currentThread()>");
        currentThreadReturn = currentThread.getReturnVariables()
                .iterator()
                .next();
    }

    @Override
    public void initialize() {
        if (!Options.get().analyzeImplicitEntries()) {
            return;
        }
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
        threadGroupInit.getParam(0).ifPresent(param0 ->
                pta.addPointsTo(context, param0, context, systemThreadGroup));
        // propagate "main" to param1
        Obj main = env.getStringConstant("main");
        threadGroupInit.getParam(1).ifPresent(param1 ->
                pta.addPointsTo(context, param1, context, main));

        // setup main thread
        // propagate <main-thread> to <java.lang.Thread: void
        //   <init>(java.lang.ThreadGroup,java.lang.String)>/this
        Obj mainThread = env.getMainThread();
        Method threadInit = pm.getUniqueMethodBySignature(
                "<java.lang.Thread: void <init>(java.lang.ThreadGroup,java.lang.String)>");
        initThis = threadInit.getThis();
        pta.addPointsTo(context, initThis, context, mainThread);
        // propagate <main-thread-group> to param0
        threadInit.getParam(0).ifPresent(param0 ->
                pta.addPointsTo(context, param0, context, mainThreadGroup));
        // propagate "main" to param1
        threadInit.getParam(1).ifPresent(param1 ->
                pta.addPointsTo(context, param1, context, main));

        // The main thread is never explicitly started, which would make it a
        // RunningThread. Therefore, we make it a running thread explicitly.
        runningThreads.addObject(
                pta.getCSManager().getCSObj(context, mainThread));
    }

    @Override
    public void handleNewPointsToSet(CSVariable csVar, PointsToSet pts) {
        if (csVar.getVariable().equals(threadStartThis)) {
            // Add new reachable thread objects to set of running threads,
            // and propagate the thread objects to return variable of
            // Thread.currentThread().
            // Since multiple threads may execute this method and
            // this.signalNewCSMethod(), we need to synchronize reads/writes
            // on runningThreads and currentThreadContexts, so we put these
            // operations in synchronized block.
            // Note that this *only* blocks when Thread.start()/@this change,
            // which is rare, thur, it should not affect concurrency much.
            synchronized (this) {
                if (runningThreads.addAll(pts)) {
                    currentThreadContexts.forEach(context ->
                            pta.addPointsTo(context, currentThreadReturn, pts));
                }
            }
        }
    }

    @Override
    public void handleNewCSMethod(CSMethod csMethod) {
        if (csMethod.getMethod().equals(currentThread)) {
            // When a new CS Thread.currentThread() is reachable, we propagate
            // all running threads to its return variable.
            // Ideally, we should only return the real *current* thread object,
            // which may require complicated thread analysis. So currently,
            // we just return all running threads for soundness.
            synchronized (this) {
                Context context = csMethod.getContext();
                currentThreadContexts.add(context);
                pta.addPointsTo(context, currentThreadReturn, runningThreads);
            }
        }
    }
}
