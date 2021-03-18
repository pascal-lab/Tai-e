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

import pascal.taie.World;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.Var;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.natives.NativeModel;
import pascal.taie.analysis.pta.PTAOptions;
import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.core.solver.PointerAnalysis;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.analysis.pta.pts.PointsToSetFactory;

import java.util.Set;

import static pascal.taie.language.classes.StringReps.STRING;
import static pascal.taie.util.CollectionUtils.getOne;
import static pascal.taie.util.CollectionUtils.newHybridSet;

/**
 * Model initialization of main thread, system thread group,
 * and some Thread APIs.
 */
public class ThreadHandler implements Plugin {

    private PointerAnalysis pta;

    private ClassHierarchy hierarchy;

    /**
     * This variable of Thread.start().
     */
    private Var threadStartThis;
    /**
     * Set of running threads.
     */
    private final PointsToSet runningThreads = PointsToSetFactory.make();
    /**
     * Represent Thread.currentThread.
     */
    private JMethod currentThread;
    /**
     * Return variable of Thread.currentThread().
     */
    private Var currentThreadReturn;
    /**
     * Contexts of Thread.currentThread().
     */
    private final Set<Context> currentThreadContexts = newHybridSet();

    @Override
    public void setPointerAnalysis(PointerAnalysis pta) {
        this.pta = pta;
        hierarchy = pta.getHierarchy();
        threadStartThis = hierarchy.getJREMethod(
                "<java.lang.Thread: void start()>")
                .getIR()
                .getThis();
        currentThread = hierarchy.getJREMethod(
                "<java.lang.Thread: java.lang.Thread currentThread()>");
        currentThreadReturn = getOne(currentThread
                .getIR()
                .getReturnVars());
    }

    @Override
    public void initialize() {
        if (!PTAOptions.get().analyzeImplicitEntries()) {
            return;
        }
        NativeModel nativeModel = World.getNativeModel();
        Context context = pta.getContextSelector().getDefaultContext();

        // setup system thread group
        // propagate <system-thread-group> to <java.lang.ThreadGroup: void <init>()>/this
        Obj systemThreadGroup = nativeModel.getSystemThreadGroup();
        IR threadGroupInitIR = hierarchy.getJREMethod(
                "<java.lang.ThreadGroup: void <init>()>")
                .getIR();
        Var initThis = threadGroupInitIR.getThis();
        pta.addVarPointsTo(context, initThis, context, systemThreadGroup);

        // setup main thread group
        // propagate <main-thread-group> to <java.lang.ThreadGroup: void
        //   <init>(java.lang.ThreadGroup,java.lang.String)>/this
        Obj mainThreadGroup = nativeModel.getMainThreadGroup();
        threadGroupInitIR = hierarchy.getJREMethod(
                "<java.lang.ThreadGroup: void <init>(java.lang.ThreadGroup,java.lang.String)>")
                .getIR();

        initThis = threadGroupInitIR.getThis();
        pta.addVarPointsTo(context, initThis, context, mainThreadGroup);
        // propagate <system-thread-group> to param0
        pta.addVarPointsTo(context, threadGroupInitIR.getParam(0),
                context, systemThreadGroup);
        // propagate "main" to param1
        Obj main = pta.getHeapModel()
                .getConstantObj(World.getTypeManager()
                        .getClassType(STRING),
                        "main");
        pta.addVarPointsTo(context, threadGroupInitIR.getParam(1), context, main);

        // setup main thread
        // propagate <main-thread> to <java.lang.Thread: void
        //   <init>(java.lang.ThreadGroup,java.lang.String)>/this
        Obj mainThread = nativeModel.getMainThread();
        IR threadInitIR = hierarchy.getJREMethod(
                "<java.lang.Thread: void <init>(java.lang.ThreadGroup,java.lang.String)>")
                .getIR();
        initThis = threadInitIR.getThis();
        pta.addVarPointsTo(context, initThis, context, mainThread);
        // propagate <main-thread-group> to param0
        pta.addVarPointsTo(context, threadInitIR.getParam(0),
                context, mainThreadGroup);
        // propagate "main" to param1
        pta.addVarPointsTo(context, threadInitIR.getParam(1), context, main);

        // The main thread is never explicitly started, which would make it a
        // RunningThread. Therefore, we make it a running thread explicitly.
        runningThreads.addObject(
                pta.getCSManager().getCSObj(context, mainThread));
    }

    @Override
    public void handleNewPointsToSet(CSVar csVar, PointsToSet pts) {
        if (csVar.getVar().equals(threadStartThis)) {
            // Add new reachable thread objects to set of running threads,
            // and propagate the thread objects to return variable of
            // Thread.currentThread().
            // Since multiple threads may execute this method and
            // this.handleNewCSMethod(), we need to synchronize reads/writes
            // on runningThreads and currentThreadContexts, so we put these
            // operations in synchronized block.
            // Note that this *only* blocks when Thread.start()/@this change,
            // which is rare, thur, it should not affect concurrency much.
            synchronized (this) {
                if (runningThreads.addAll(pts)) {
                    currentThreadContexts.forEach(context ->
                            pta.addVarPointsTo(context, currentThreadReturn, pts));
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
                pta.addVarPointsTo(context, currentThreadReturn, runningThreads);
            }
        }
    }
}
