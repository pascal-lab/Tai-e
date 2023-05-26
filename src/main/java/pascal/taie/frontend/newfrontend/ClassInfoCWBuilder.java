package pascal.taie.frontend.newfrontend;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.JavaClass;
import org.objectweb.asm.ClassReader;
import pascal.taie.project.AnalysisFile;
import pascal.taie.project.ClassFile;
import pascal.taie.project.DirContainer;
import pascal.taie.project.FileContainer;
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
            if (entry != null) {
                buildClosure(entry);
            }
            for (var i : p.getInputClasses()) {
                buildClosure(i);
            }
            for (var container : p.getAppRootContainers()) {
                buildClosure(container, "");
            }
        } catch (IOException e) {
            // TODO: fail info
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private void buildClosure(FileContainer container, String packageString) throws IOException {
        for (var f : container.files()) {
            if (f instanceof JavaSourceFile jFile) {
                // TODO: fill here
            } else if (f instanceof ClassFile cFile) {
                if (cFile.className().contains("android")) { // TODO: workaround
                    continue;
                }
                var deps = buildClassDeps(packageString + cFile.className(), cFile);
                for (String dep : deps) {
                    buildClosure(dep);
                }
            }
        }
        for (var c : container.containers()) {
            regardOnlyDirAsRestClassPath(c, packageString);
        }
    }

    private void regardOnlyDirAsRestClassPath(FileContainer subContainer, String currentPackageString) throws IOException {
        if (subContainer instanceof DirContainer) {
            buildClosure(subContainer, currentPackageString + subContainer.className() + ".");
        }
    }

    private void regardJarAndZipAsRestClassPathToo(FileContainer subContainer, String currentPackageString) throws IOException {
        buildClosure(subContainer, currentPackageString + subContainer.className() + ".");
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
        boolean isApplication = project.isApp(cFile)
                || project.getInputClasses().contains(binaryName)
                || binaryName.equals(project.getMainClass());
        byte[] content = cFile.resource().getContent();
        ClassReader reader = new ClassReader(content);
        // DepClassVisitor v = new DepClassVisitor();
        sourceMap.put(binaryName, new AsmSource(reader, isApplication));
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
