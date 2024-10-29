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

package pascal.taie.frontend.newfrontend.closedworld;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import pascal.taie.frontend.newfrontend.exception.AsmFrontendException;
import pascal.taie.frontend.newfrontend.source.ClassSource;
import pascal.taie.frontend.newfrontend.exception.FrontendException;
import pascal.taie.frontend.newfrontend.FrontendOptions;
import pascal.taie.frontend.newfrontend.report.StageTimer;
import pascal.taie.World;
import pascal.taie.project.AnalysisFile;
import pascal.taie.project.Project;
import pascal.taie.project.SearchIndex;
import pascal.taie.util.Timer;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Pair;
import pascal.taie.util.collection.Sets;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
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

public class DependencyCWBuilder implements ClosedWorldBuilder {

    private static final String BASIC_CLASSES = "basic-classes.yml";

    // TODO: load FileSystem, ignore others
    private static final List<String> basicClassesList = loadBasicClasses();

    private static final int THREAD_POOL_SIZE = 8;

    private ConcurrentMap<String, ClassSource> sourceMap;

    private final ExecutorService executorService;

    private final CompletionService<ResolveResult> completionService;

    private Project project;

    private SearchIndex index;

    public DependencyCWBuilder() {
        int threadPoolSize = FrontendOptions.get().isUseParallelHierarchy()
                ? THREAD_POOL_SIZE
                : 1;
        sourceMap = Maps.newConcurrentMap();
        executorService = Executors.newFixedThreadPool(threadPoolSize);
        completionService = new ExecutorCompletionService<>(executorService);
    }

    @Override
    public int getTotalClasses() {
        return sourceMap.size();
    }

    @Override
    public Collection<ClassSource> getClosedWorld() {
        var v = sourceMap.values();
        sourceMap = null;
        return v;
    }

    public void addTargets(List<String> target, List<String> binaryNames) {
        for (String s : binaryNames) {
            target.add(s.replace('.', '/'));
        }
    }

    @Override
    public void build(Project p) {
        Timer timer = new Timer("closed-world");
        timer.start();
        String entry = p.getMainClass();
        this.project = p;
        List<String> target = new ArrayList<>();
        try {
            index = SearchIndex.makeIndex(project);
            if (entry != null) {
                addTargets(target, List.of(entry));
            }
            addTargets(target, p.getInputClasses());
            addTargets(target, loadNecessaryClasses());
            if (World.get().getOptions().getUseNonParallelCWAlgorithm()) {
                buildClosureNonParallel(target);
            } else {
                buildClosure(target);
            }
            timer.stop();
            StageTimer.getInstance().reportCWTime((long) (timer.inSecond() * 1000));
        } catch (IOException e) {
            // TODO: fail info
            throw new FrontendException(e.getMessage());
        } finally {
            // call it explicitly, or the program will not exit after main() returns
            executorService.shutdown();
        }
    }

    private void buildClosure(Collection<String> binaryNames) throws IOException {
        Queue<String> workList = new LinkedList<>(binaryNames);
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
                String binaryName = workList.poll();
                if (!founded.contains(binaryName)) {
                    founded.add(binaryName);
                    completionService.submit(() -> buildDeps(binaryName));
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
                    try {
                        Future<ResolveResult> future = completionService.take();
                        ResolveResult r = future.get();
                        updateIteration(founded, workList, r);
                        deltaCount--;
                    } catch (ExecutionException | InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    private void buildClosureNonParallel(List<String> binaryNames) {
        Queue<String> workList = new LinkedList<>(binaryNames);
        Set<String> founded = Sets.newHybridSet();

        while (! workList.isEmpty()) {
            String now = workList.poll();
            if (! founded.contains(now)) {
                founded.add(now);
                try {
                    ResolveResult r = buildDeps(now);
                    updateIteration(founded, workList, r);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void updateIteration(Set<String> founded, Queue<String> workList, ResolveResult r) {
        if (r == null) {
            return;
        } else {
            for (Pair<String, ClassSource> resolved : r.resolvedSource()) {
                founded.add(resolved.first());
            }
            workList.addAll(r.dependencies());
        }
    }

    private ResolveResult buildDeps(String binaryName) throws IOException {
        AnalysisFile f = index.locate(binaryName);
        if (f == null) {
            if (basicClassesList.contains(binaryName.replace('/', '.'))) {
                // if some classes in basicClassesList are not found, then just ignore them
                // for that we use try-catch style here to load them.
                // E.g. you usually only load one of the three concrete FileSystem s.
                return null;
            }
            if (World.get().getOptions().isAllowPhantom()) {
                return null;
            }
            throw new FileNotFoundException(binaryName);
        } else {
             ResolveResult depAndSources =
                    DependencyResolver.resolve(project, binaryName, f);
            for (Pair<String, ClassSource> source : depAndSources.resolvedSource()) {
                sourceMap.put(source.first(), source.second());
            }
            return depAndSources;
        }
    }


    private List<String> loadNecessaryClasses() {
        return List.of("java.lang.ref.Finalizer");
    }

    /**
     * Reads basic classes specified by file {@link #BASIC_CLASSES}
     */
    private static List<String> loadBasicClasses() {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        JavaType type = mapper.getTypeFactory()
                .constructCollectionType(List.class, String.class);
        try {
            InputStream content = DependencyCWBuilder.class
                    .getClassLoader()
                    .getResourceAsStream(BASIC_CLASSES);
            return mapper.readValue(content, type);
        } catch (IOException e) {
            throw new AsmFrontendException("Failed to read newfrontend basic classes", e);
        }
    }
}
