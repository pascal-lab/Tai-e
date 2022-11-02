package pascal.taie.frontend.newfrontend;

import org.objectweb.asm.ClassReader;
import pascal.taie.project.AnalysisFile;
import pascal.taie.project.ClassFile;
import pascal.taie.project.JavaSourceFile;
import pascal.taie.project.Project;
import pascal.taie.util.collection.Maps;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class DepCWBuilder implements ClosedWorldBuilder {

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
            buildClosure(entry);
        } catch (IOException e) {
            // TODO: fail info
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private void buildClosure(String binaryName) throws IOException {
        if (sourceMap.containsKey(binaryName)) {
            return;
        }

        AnalysisFile f = project.locate(binaryName);
        if (f == null) {
            throw new FileNotFoundException(binaryName);
        }

        List<String> deps = null;
        if (f instanceof JavaSourceFile jFile) {
            // TODO: fill here
        } else if (f instanceof ClassFile cFile) {
            deps = buildClassDeps(cFile);
        } else {
            throw new IllegalStateException();
        }

        for (var i : deps) {
            buildClosure(i);
        }
    }

    private List<String> buildClassDeps(ClassFile cFile) throws IOException {
        byte[] content = cFile.resource().getContent();
        ClassReader reader = new ClassReader(content);
        DepClassVisitor v = new DepClassVisitor();
        reader.accept(v, ClassReader.SKIP_CODE);
        return v.getBinaryNames().stream().toList();
    }
}
