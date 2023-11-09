package pascal.taie.frontend.newfrontend;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import org.objectweb.asm.tree.ClassNode;
import pascal.taie.frontend.newfrontend.java.JavaClassManager;
import pascal.taie.World;
import pascal.taie.project.AnalysisFile;
import pascal.taie.project.ClassFile;
import pascal.taie.project.JavaSourceFile;
import pascal.taie.project.Project;
import pascal.taie.project.SearchIndex;
import pascal.taie.util.Timer;
import pascal.taie.util.collection.Maps;
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
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class DepCWBuilder implements ClosedWorldBuilder {

    private static final String BASIC_CLASSES = "basic-classes.yml";

    // TODO: load FileSystem, ignore others
    private static final List<String> basicClassesList = loadBasicClasses();

    private static final int THREAD_POOL_SIZE = 8;

    private ConcurrentMap<String, ClassSource> sourceMap;

    private final CompletionService<Completed> completionService;

    private final boolean preBuildIR;

    private Project project;

    private SearchIndex index;

    public DepCWBuilder() {
        sourceMap = Maps.newConcurrentMap();
        Executor executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        completionService = new ExecutorCompletionService<>(executor);
        preBuildIR = World.get().getOptions().isPreBuildIR();
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
                    completionService.submit(() ->
                            new Completed(binaryName, buildDeps(binaryName)));
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
                        Future<Completed> future = completionService.take();
                        Completed res = future.get();
                        workList.addAll(res.res);
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
                    Collection<String> deps = buildDeps(now);
                    workList.addAll(deps);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private Collection<String> buildDeps(String binaryName) throws IOException {
        AnalysisFile f = index.locate(binaryName);
        if (f == null) {
            if (basicClassesList.contains(binaryName.replace('/', '.'))) {
                // if some classes in basicClassesList are not found, then just ignore them
                // for that we use try-catch style here to load them.
                // E.g. you usually only load one of the three concrete FileSystem s.
                return List.of();
            }
            if (World.get().getOptions().isAllowPhantom()) {
                return List.of();
            }
            throw new FileNotFoundException(binaryName);
        }

        if (f instanceof JavaSourceFile javaSourceFile) {
//            logger.warn(
//                    "WARNING: currently new frontend does not support java source code("
//                    + ((JavaSourceFile) f).getClassName()
//                    + "). So this class would be ignored and the rest task continues on.");

            // DO NOT change the order of next 2 stmts
            List<String> deps = JavaClassManager.get().getImports(project, javaSourceFile);
            JavaSource[] sources = JavaClassManager.get().getJavaSources(javaSourceFile);
            for (JavaSource s : sources) {
                sourceMap.put(s.getClassName(), s);
            }
            return deps;
        } else if (f instanceof ClassFile classFile) {
            return buildClassDeps(binaryName, classFile);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private Collection<String> buildClassDeps(String binaryName, ClassFile cFile) throws IOException {
        boolean isApplication = project.isApp(cFile)
//                || project.getInputClasses().contains(binaryName)
                || binaryName.equals(project.getMainClass());
        byte[] content = cFile.resource().getContent();
        cFile.resource().release();
        assert content != null;
        ClassReader reader = new ClassReader(content);
//        assert reader.getClassName().replaceAll("/", ".").equals(binaryName);
        int version = reader.readShort(6);
        if (!preBuildIR) {
            sourceMap.put(binaryName, new AsmSource(reader, isApplication, version, null));
        } else {
            ClassNode classNode = new ClassNode(Opcodes.ASM9) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor,
                                                 String signature, String[] exceptions) {
                    JSRInlinerAdapter adapter =
                            new JSRInlinerAdapter(null, access, name, descriptor, signature, exceptions);
                    methods.add(adapter);
                    return adapter;
                }
            };
            sourceMap.put(binaryName, new AsmSource(null, isApplication, version, classNode));
            reader.accept(classNode, ClassReader.EXPAND_FRAMES);
        }
        return new ConstantTableReader(content).read();
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
            InputStream content = DepCWBuilder.class
                    .getClassLoader()
                    .getResourceAsStream(BASIC_CLASSES);
            return mapper.readValue(content, type);
        } catch (IOException e) {
            throw new AsmFrontendException("Failed to read newfrontend basic classes", e);
        }
    }

    private record Completed(String input, Collection<String> res) {}
}
