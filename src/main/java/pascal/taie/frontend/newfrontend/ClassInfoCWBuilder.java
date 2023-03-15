package pascal.taie.frontend.newfrontend;

import org.apache.bcel.classfile.*;
import org.objectweb.asm.ClassReader;
import pascal.taie.project.AnalysisFile;
import pascal.taie.project.ClassFile;
import pascal.taie.project.JavaSourceFile;
import pascal.taie.project.Project;
import pascal.taie.util.collection.Maps;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class ClassInfoCWBuilder implements ClosedWorldBuilder {

    private final Map<String, ClassSource> sourceMap;

    private Project project;

    public ClassInfoCWBuilder() {
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
        byte[] content = cFile.resource().getContent();
        ClassReader reader = new ClassReader(content);
        // DepClassVisitor v = new DepClassVisitor();
        sourceMap.put(binaryName, new AsmSource(reader));
        // reader.accept(v, ClassReader.SKIP_FRAMES);
        //return v.getBinaryNames().stream().toList();

        InputStream inputStream = new ByteArrayInputStream(content);
        ClassParser classParser = new ClassParser(inputStream, binaryName);
        JavaClass clazz = classParser.parse();
        ConstantPool constantPool = clazz.getConstantPool();
        return Arrays.stream(constantPool.getConstantPool())
                .filter(c -> c instanceof ConstantClass)
                .map(c -> (ConstantClass) c)
                .map(constantPool::constantToString)
                .filter(s -> s.charAt(0) != '[') // array types start with '[', which should be ignored.
                .toList();
    }
}
