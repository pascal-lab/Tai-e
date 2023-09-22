package pascal.taie.frontend.newfrontend.java;


import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FileASTRequestor;
import pascal.taie.World;
import pascal.taie.frontend.newfrontend.JavaSource;
import pascal.taie.project.AnalysisFile;
import pascal.taie.project.FileContainer;
import pascal.taie.project.FileResource;
import pascal.taie.project.JarContainer;
import pascal.taie.project.JavaSourceFile;
import pascal.taie.project.Project;
import pascal.taie.project.Resource;
import pascal.taie.util.collection.Streams;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;


public class JavaClassManager {

    private final Map<JavaSourceFile, CompilationUnit> class2Source;

    private final Map<String, JavaSourceFile> sourceFileMap;

    private ASTParser parser;

    private Project project;

    private boolean parsed;

    private static JavaClassManager manager;

    public static JavaClassManager get() {
        if (manager == null) {
            manager = new JavaClassManager();
        }
        return manager;
    }

    private JavaClassManager() {
        class2Source = new HashMap<>();
        sourceFileMap = new HashMap<>();
        parsed = false;
    }

    public List<String> getImports(Project p, JavaSourceFile file) {
        if (project == null) {
            project = p;
        }

        CompilationUnit unit = parseSourceCode(file);
        ImportExtractor extractor = new ImportExtractor();
        unit.accept(extractor);
        return extractor.getDependencies();
    }

    public JavaSource getJavaSource(JavaSourceFile file) {
        CompilationUnit unit = parseSourceCode(file);
        return new JavaSource(unit);
    }

    private CompilationUnit parseSourceCode(JavaSourceFile file) {
        if (parsed) {
            return class2Source.get(file);
        } else {
            invokeParser();
            return parseSourceCode(file);
        }
    }

    private void invokeParser() {
        createParser();
        String[] cp = getClassPath();
        String[] sourceFiles = getAllJavaSources();
        // TODO: currently not able to handle java >= 9
        parser.setEnvironment(cp, null, null, false);
        parser.createASTs(sourceFiles, null, new String[0], new FileASTRequestor() {
            @Override
            public void acceptAST(String sourceFilePath, CompilationUnit ast) {
                JavaSourceFile file = sourceFileMap.get(sourceFilePath);
                assert file != null;
                class2Source.put(file, ast);
            }
        }, new NullProgressMonitor());
        parsed = true;
    }

    private void createParser() {
        ASTParser parser = ASTParser.newParser(AST.JLS17);
        parser.setResolveBindings(true);
        // TODO: if phantom enable, set true
        parser.setBindingsRecovery(false);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        var options = JavaCore.getOptions();
        options.put(JavaCore.COMPILER_SOURCE, "1.7");
        parser.setCompilerOptions(options);
        this.parser = parser;
    }

    private String[] getAllJavaSources() {
        List<JavaSourceFile> files = outputAll(project.getAppRootContainers());
        files.addAll(outputAll(project.getLibRootContainers()));

        List<String> paths = new ArrayList<>();
        for (JavaSourceFile file : files) {
            getSourcePath(file).ifPresent(p -> {
                sourceFileMap.put(p, file);
                paths.add(p);
            });
        }

        return paths.toArray(new String[0]);
    }

    private String[] getClassPath() {
        // TODO: add real paths;
        String JREs = "java-benchmarks/JREs";
        int javaVersion = World.get().getOptions().getJavaVersion();
        String[] cps = World.get().getOptions().getClassPath().split(File.pathSeparator);
        List<String> res = new ArrayList<>(List.of(cps));
        if (javaVersion <= 8) {
            String jrePath = String.format("%s/jre" + "1.%d",
                    JREs, javaVersion);
            try (Stream<Path> paths = Files.walk(Path.of(jrePath))) {
                paths.forEach(p -> {
                    if (p.toString().endsWith(".jar")) {
                        res.add(p.toString());
                    }
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return res.toArray(new String[0]);
    }

    private List<JavaSourceFile> outputAll(FileContainer container) {
        List<JavaSourceFile> res = new ArrayList<>();
        for (AnalysisFile file : container.files()) {
            if (file instanceof JavaSourceFile sourceFile) {
                res.add(sourceFile);
            }
        }
        res.addAll(outputAll(container.containers()));
        return res;
    }

    private List<JavaSourceFile> outputAll(List<FileContainer> containers) {
        List<JavaSourceFile> res = new ArrayList<>();
        for (FileContainer container1 : containers) {
            res.addAll(outputAll(container1));
        }
        return res;
    }

    private Optional<String> getSourcePath(JavaSourceFile file) {
        Resource r = file.resource();
        if (r instanceof FileResource) {
            return Optional.of(r.getPath().toString());
        } else {
            // TODO: add warning or try to handle it
            return Optional.empty();
        }
    }
}
