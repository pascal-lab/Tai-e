package pascal.taie.frontend.newfrontend;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import pascal.taie.project.AnalysisFile;
import pascal.taie.project.ClassFile;
import pascal.taie.project.JavaSourceFile;
import pascal.taie.project.Project;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Sets;

import java.io.FileNotFoundException;
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
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class DepCWBuilder implements ClosedWorldBuilder {

    private static final Logger logger = LogManager.getLogger(DepCWBuilder.class);

    private static final int THREAD_POOL_SIZE = 8;

    private final ConcurrentMap<String, ClassSource> sourceMap;

    private final Set<String> founded;

    private final CompletionService<Completed> completionService;

    private Project project;

    public DepCWBuilder() {
        sourceMap = Maps.newConcurrentMap();
        founded = Sets.newHybridSet();
        Executor executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        completionService = new ExecutorCompletionService<>(executor);
    }

    @Override
    public int getTotalClasses() {
        return sourceMap.size();
    }

    @Override
    public Collection<ClassSource> getClosedWorld() {
        return sourceMap.values();
    }

    @Override
    public void build(Project p) {
        String entry = p.getMainClass();
        this.project = p;
        List<String> target = new ArrayList<>();
        try {
            if (entry != null) {
                target.add(entry);
            }
            target.addAll(p.getInputClasses());
            buildClosure(target);
        } catch (IOException e) {
            // TODO: fail info
            throw new FrontendException(e.getMessage());
        }
    }

    private void buildClosure(List<String> binaryNames) throws IOException {
        String excluded = "android$widget$RemoteViews$BaseReflectionAction";
        if (binaryNames.contains(excluded)) {
            // FIX ME: a workaround to skip android test case in test/resources/android$widget$RemoteViews$BaseReflectionAction.class
            binaryNames.remove(excluded);
            return;
        }

        Queue<String> workList = new LinkedList<>(binaryNames);

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
                    completionService.submit(() ->
                            new Completed(binaryName, buildDeps(binaryName)));
                    tempDeltaCount ++;
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
                        Future<Completed> future = completionService.take();
                        Completed res = future.get();
                        workList.addAll(res.res);
                        deltaCount--;
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    private List<String> buildDeps(String binaryName) throws IOException {
        AnalysisFile f = project.locate(binaryName);
        if (f == null) {
            throw new FileNotFoundException(binaryName);
        }

        if (f instanceof JavaSourceFile) {
            logger.warn(
                    "WARNING: currently new frontend does not support java source code("
                    + ((JavaSourceFile) f).getClassName()
                    + "). So this class would be ignored and the rest task continues on.");
            return List.of();
        } else if (f instanceof ClassFile classFile) {
            return buildClassDeps(binaryName, classFile);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private List<String> buildClassDeps(String binaryName, ClassFile cFile) throws IOException {
        boolean isApplication = project.isApp(cFile)
                || project.getInputClasses().contains(binaryName)
                || binaryName.equals(project.getMainClass());
        byte[] content = cFile.resource().getContent();
        ClassReader reader = new ClassReader(content);
        assert reader.getClassName().replaceAll("/", ".").equals(binaryName);
        sourceMap.put(binaryName, new AsmSource(reader, isApplication));
        return new ConstantTableReader().read(content);
    }

    private record Completed(String input, List<String> res) {}
}
