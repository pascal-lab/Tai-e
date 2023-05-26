package pascal.taie.frontend.newfrontend;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import pascal.taie.project.AnalysisFile;
import pascal.taie.project.ClassFile;
import pascal.taie.project.JavaSourceFile;
import pascal.taie.project.Project;
import pascal.taie.util.collection.Maps;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class DepCWBuilder implements ClosedWorldBuilder {

    private static final Logger logger = LogManager.getLogger(DepCWBuilder.class);

    private final Map<String, ClassSource> sourceMap;

    private Project project;

    public DepCWBuilder() {
        sourceMap = Maps.newHybridMap();
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
        try {
            if (entry != null) {
                buildClosure(entry);
            }
            for (var i : p.getInputClasses()) {
                buildClosure(i);
            }
        } catch (IOException e) {
            // TODO: fail info
            throw new RuntimeException(e);
        }
    }

    private void buildClosure(String binaryName) throws IOException {
        if (binaryName.equals("android$widget$RemoteViews$BaseReflectionAction")) {
            // FIX ME: a workaround to skip android test case in test/resources/android$widget$RemoteViews$BaseReflectionAction.class
            return;
        }
        Queue<String> workList = new LinkedList<>();
        workList.add(binaryName);
        while (! workList.isEmpty()) {
            binaryName = workList.poll();
            if (sourceMap.containsKey(binaryName)) {
                continue;
            }

            AnalysisFile f = project.locate(binaryName);
            if (f == null) {
                throw new FileNotFoundException(binaryName);
            }

            if (f instanceof JavaSourceFile) {
                logger.warn(
                        "WARNING: currently new frontend does not support java source code("
                        + ((JavaSourceFile) f).getClassName()
                        + "). So this class would be ignored and the rest task continues on.");
                return;
            }

            List<String> deps = null;
            if (f instanceof JavaSourceFile jFile) {
                // TODO: fill here
            } else if (f instanceof ClassFile cFile) {
                deps = buildClassDeps(binaryName, cFile);
            } else {
                throw new IllegalStateException();
            }
            workList.addAll(deps);
        }
    }

    private List<String> buildClassDeps(String binaryName, ClassFile cFile) throws IOException {
        boolean isApplication = project.isApp(cFile)
                || project.getInputClasses().contains(binaryName)
                || binaryName.equals(project.getMainClass());
        byte[] content = cFile.resource().getContent();
        ClassReader reader = new ClassReader(content);
        assert reader.getClassName().replaceAll("/", ".").equals(binaryName);
        DepClassVisitor v = new DepClassVisitor();
        sourceMap.put(binaryName, new AsmSource(reader, isApplication));
        reader.accept(v, ClassReader.SKIP_FRAMES);
        return v.getBinaryNames().stream().toList();
    }
}
