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

package pascal.taie.frontend.java.closedworld;

import pascal.taie.World;
import pascal.taie.frontend.java.exception.FrontendException;
import pascal.taie.frontend.java.source.ClassSource;
import pascal.taie.project.ClassFile;
import pascal.taie.project.ClassIndex;
import pascal.taie.project.Project;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Sets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Build the closed world via reference analysis.
 */
public class ClosedWorldBuilder {

    private static final Set<String> BASIC_CLASSES = Set.of(
            // For simulating the FileSystem class, we need the implementation
            // of the FileSystem, but the classes are not loaded automatically
            // due to the indirection via native code.
            "java.io.UnixFileSystem",
            "java.io.WinNTFileSystem",
            "java.io.Win32FileSystem",
            "java.lang.ref.Finalizer"
    );

    private static final int THREAD_POOL_SIZE = 8;

    private final Project project;

    private final ExecutorService executorService =
            Executors.newFixedThreadPool(THREAD_POOL_SIZE);

    private final CompletionService<ResolveResult> completionService =
            new ExecutorCompletionService<>(executorService);

    private ClassIndex index;

    private ConcurrentMap<String, ClassSource> sourceMap;

    public ClosedWorldBuilder(Project project) {
        this.project = project;
    }

    public Collection<ClassSource> build() throws FrontendException {
        sourceMap = Maps.newConcurrentMap();
        index = project.makeIndex();
        // initialize starting point of closure construction
        List<String> initialClasses = new ArrayList<>();
        String entry = project.mainClass();
        if (entry != null) {
            initialClasses.add(entry);
        }
        initialClasses.addAll(project.inputClasses());
        initialClasses.addAll(BASIC_CLASSES);
        try {
            buildClosure(initialClasses);
        } catch (InterruptedException ex) {
            throw new FrontendException(ex);
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof FrontendException fex) {
                throw fex;
            } else {
                throw new FrontendException(cause);
            }
        } finally {
            // call it explicitly, otherwise the program will not stop
            // even after main() returns
            executorService.shutdown();
        }
        var classes = sourceMap.values();
        sourceMap = null;
        index = null;
        return classes;
    }

    private void buildClosure(Collection<String> classNames)
            throws ExecutionException, InterruptedException {
        Queue<String> workList = new LinkedList<>(classNames);
        Set<String> founded = Sets.newHybridSet();

        // deltaCount means (founded, not completed) - completed, a.k.a "on the fly"
        // it MUST be equal to the task that
        // completionService are current running (not retried by user)
        int deltaCount = 0;

        // this loop will satisfy:
        // deltaCount_{before} < deltaCount_{after} \/ tempDeltaCount == 0
        while (! workList.isEmpty() || deltaCount > 0) {

            // first, poll all classes from workList
            // They will be built in parallel
            int tempDeltaCount = 0;
            while (! workList.isEmpty()) {
                String className = workList.poll();
                if (!founded.contains(className)) {
                    founded.add(className);
                    completionService.submit(() -> resolveDeps(className));
                    tempDeltaCount++;
                }
            }
            deltaCount += tempDeltaCount;

            // if there are enough classes to wait for,
            // or this iteration do not found new class
            //    (which means you have to process all remaining classes)
            // wait them for build completed
            if (deltaCount >= THREAD_POOL_SIZE || tempDeltaCount == 0) {
                while (deltaCount > 0) {
                    Future<ResolveResult> future = completionService.take();
                    ResolveResult result = future.get();
                    updateIteration(result, founded, workList);
                    deltaCount--;
                }
            }
        }
    }

    private void updateIteration(ResolveResult result,
                                 Set<String> founded, Queue<String> workList) {
        if (result != null) {
            for (ClassSource resolved : result.resolvedSource()) {
                founded.add(resolved.getClassName());
            }
            workList.addAll(result.dependencies());
        }
    }

    private ResolveResult resolveDeps(String className)
            throws IOException, FrontendException {
        ClassFile classFile = index.find(className);
        if (classFile == null) {
            if (BASIC_CLASSES.contains(className)) {
                // if some classes in BASIC_CLASSES are not found, then just
                // ignore them for that we use try-catch here to load them,
                // e.g. only load one of the three concrete FileSystem.
                return null;
            }
            if (World.get().getOptions().isAllowPhantom()) {
                ResolveResult result = DependencyResolver.resolvePhantom(className);
                addClassSource(result);
                return result;
            }
            throw new FrontendException(
                    className + " is not found in the given classpath");
        } else {
             ResolveResult result = DependencyResolver.resolve(project, classFile);
             addClassSource(result);
             return result;
        }
    }

    private void addClassSource(ResolveResult result) {
        for (ClassSource source : result.resolvedSource()) {
            sourceMap.put(source.getClassName(), source);
        }
    }
}
