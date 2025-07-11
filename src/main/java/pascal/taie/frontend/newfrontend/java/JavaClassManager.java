package pascal.taie.frontend.newfrontend.java;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FileASTRequestor;
import pascal.taie.World;
import pascal.taie.frontend.newfrontend.JavaSource;
import pascal.taie.project.AnalysisFile;
import pascal.taie.project.FileContainer;
import pascal.taie.project.FileResource;
import pascal.taie.project.JavaSourceFile;
import pascal.taie.project.Project;
import pascal.taie.project.Resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
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

    private final static Logger logger = LogManager.getLogger(JavaClassManager.class);

    public static JavaClassManager get() {
        if (manager == null) {
            manager = new JavaClassManager();
        }
        return manager;
    }

    public static void reset() {
        manager = null;
    }

    static {
        World.registerResetCallback(JavaClassManager::reset);
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

    public JavaSource[] getJavaSources(JavaSourceFile file) {
        CompilationUnit unit = parseSourceCode(file);
        ClassExtractor extractor = new ClassExtractor();
        unit.accept(extractor);
        return extractor.getTypeDeclarations()
                .stream()
                .map(t -> new JavaSource(unit, t, extractor.getOuterClass(t)))
                .toArray(JavaSource[]::new);
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
        //  (But if `-pp` specified, JDT can handle any java version)
        parser.setEnvironment(cp, null, null,
                World.get().getOptions().isPrependJVM());
        parser.createASTs(sourceFiles, null, new String[0], new FileASTRequestor() {
            @Override
            public void acceptAST(String sourceFilePath, CompilationUnit ast) {
                JavaSourceFile file = sourceFileMap.get(sourceFilePath);
                assert file != null;
                class2Source.put(file, ast);
                logJDTError(ast);
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
        List<String> res = World.get().getOptions().getClassPath();
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

    private static void logJDTError(CompilationUnit unit) {
        List<IProblem> errors = Arrays.stream(unit.getProblems())
                .filter(IProblem::isError)
                .toList();
        if (! errors.isEmpty()) {
            String fileName = new String(errors.get(0).getOriginatingFileName());
            Path p = Paths.get(fileName);
            try {
                String sourceCode = Files.readString(p);
                for (IProblem error : errors) {
                    logger.error(makeLogError(error, unit, sourceCode, fileName));
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static String makeLogError(IProblem problem,
                                       CompilationUnit unit,
                                       String sourceCode,
                                       String fileName) {
        String message = problem.getMessage();
        int loc = problem.getSourceLineNumber();
        int sourceStart = problem.getSourceStart();
        int columnStart = unit.getColumnNumber(sourceStart);
        int sourceEnd = problem.getSourceEnd();
        int columnEnd = unit.getColumnNumber(sourceEnd);
        int lineStart = unit.getPosition(loc, 0);
        int lineEnd = unit.getPosition(loc + 1, 0);
        if (lineEnd < 0) {
            lineEnd = sourceCode.length();
        }

        String spaces = "            ";
        StringBuilder code = new StringBuilder(spaces);
        StringBuilder highLight = new StringBuilder(spaces);
        for (int i = lineStart; i < lineEnd; ++i) {
            char current = sourceCode.charAt(i);
            code.append(current);
            if (i >= sourceStart && i <= sourceEnd) {
                highLight.append(buildErrorHighlight(current, '^'));
            } else {
                highLight.append(buildErrorHighlight(current, ' '));
            }
        }
        String codeStr = code.toString();
        return "[JDT ERROR] in [" + fileName + "], line " + loc +
                "(" + columnStart + "," + columnEnd + ")" + ":" + System.lineSeparator() +
                codeStr.stripTrailing() + System.lineSeparator() +
                highLight.toString().stripTrailing() + System.lineSeparator() +
                spaces + message;
    }

    private static boolean isLatin(char c) {
        return StandardCharsets.ISO_8859_1.newEncoder().canEncode(c);
    }

    private static String buildErrorHighlight(char c, char what) {
        String res = String.valueOf(what);
        return isLatin(c) ? res : res + res;
    }
}
