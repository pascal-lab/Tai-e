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

import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.heap.Descriptor;
import pascal.taie.analysis.pta.core.heap.HeapModel;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.core.solver.EntryPoint;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.core.solver.SpecifiedParamProvider;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.exp.StringLiteral;
import pascal.taie.ir.exp.Var;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.ClassNames;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.TypeSystem;
import pascal.taie.util.collection.Sets;

import java.util.Set;

import static java.util.Objects.requireNonNull;
import static pascal.taie.util.collection.CollectionUtils.getOne;

/**
 * Models initialization of system thread group, main thread group,
 * main thread, and some Thread APIs.
 */
public class ThreadHandler implements Plugin {

    private Solver solver;

    private ClassHierarchy hierarchy;

    /**
     * This variable of Thread.start().
     */
    private Var threadStartThis;

    /**
     * Set of running threads.
     */
    private PointsToSet runningThreads;

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
    private final Set<Context> currentThreadContexts = Sets.newHybridSet();

    @Override
    public void setSolver(Solver solver) {
        this.solver = solver;
        runningThreads = solver.makePointsToSet();
        hierarchy = solver.getHierarchy();
        threadStartThis = requireNonNull(
                hierarchy.getJREMethod("<java.lang.Thread: void start()>"))
                .getIR()
                .getThis();
        currentThread = hierarchy.getJREMethod(
                "<java.lang.Thread: java.lang.Thread currentThread()>");
        currentThreadReturn = getOne(requireNonNull(currentThread)
                .getIR()
                .getReturnVars());
    }

    @Override
    public void onStart() {
        if (!solver.getOptions().getBoolean("implicit-entries")) {
            return;
        }
        TypeSystem typeSystem = solver.getTypeSystem();
        HeapModel heapModel = solver.getHeapModel();

        // setup system thread group
        JMethod threadGroupInit = requireNonNull(
                hierarchy.getJREMethod("<java.lang.ThreadGroup: void <init>()>"));
        ClassType threadGroup = typeSystem.getClassType(ClassNames.THREAD_GROUP);
        Obj systemThreadGroup = heapModel.getMockObj(Descriptor.ENTRY_DESC,
                "<system-thread-group>", threadGroup);
        solver.addEntryPoint(new EntryPoint(threadGroupInit,
                new SpecifiedParamProvider.Builder(threadGroupInit)
                        .addThisObj(systemThreadGroup)
                        .build()));

        // setup main thread group
        JMethod threadGroupInit2 = requireNonNull(
                hierarchy.getJREMethod("<java.lang.ThreadGroup: void <init>(java.lang.ThreadGroup,java.lang.String)>"));
        Obj mainThreadGroup = heapModel.getMockObj(Descriptor.ENTRY_DESC,
                "<main-thread-group>", threadGroup);
        Obj main = heapModel.getConstantObj(StringLiteral.get("main"));
        solver.addEntryPoint(new EntryPoint(threadGroupInit2,
                new SpecifiedParamProvider.Builder(threadGroupInit2)
                        .addThisObj(mainThreadGroup)
                        .addParamObj(0, systemThreadGroup)
                        .addParamObj(1, main)
                        .build()));

        // setup main thread
        JMethod threadInit = requireNonNull(
                hierarchy.getJREMethod("<java.lang.Thread: void <init>(java.lang.ThreadGroup,java.lang.String)>"));
        Obj mainThread = heapModel.getMockObj(Descriptor.ENTRY_DESC,
                "<main-thread>", typeSystem.getClassType(ClassNames.THREAD));
        solver.addEntryPoint(new EntryPoint(threadInit,
                new SpecifiedParamProvider.Builder(threadInit)
                        .addThisObj(mainThread)
                        .addParamObj(0, mainThreadGroup)
                        .addParamObj(1, main)
                        .build()));

        // The main thread is never explicitly started, which would make it a
        // RunningThread. Therefore, we make it a running thread explicitly.
        Context ctx = solver.getContextSelector().getEmptyContext();
        runningThreads.addObject(
                solver.getCSManager().getCSObj(ctx, mainThread));
    }

    @Override
    public void onNewPointsToSet(CSVar csVar, PointsToSet pts) {
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
                            solver.addVarPointsTo(context, currentThreadReturn, pts));
                }
            }
        }
    }

    @Override
    public void onNewCSMethod(CSMethod csMethod) {
        if (csMethod.getMethod().equals(currentThread)) {
            // When a new CS Thread.currentThread() is reachable, we propagate
            // all running threads to its return variable.
            // Ideally, we should only return the real *current* thread object,
            // which may require complicated thread analysis. So currently,
            // we just return all running threads for soundness.
            synchronized (this) {
                Context context = csMethod.getContext();
                currentThreadContexts.add(context);
                solver.addVarPointsTo(context, currentThreadReturn, runningThreads);
            }
        }
    }
}
