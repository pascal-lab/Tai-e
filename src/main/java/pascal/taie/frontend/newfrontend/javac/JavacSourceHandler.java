package pascal.taie.frontend.newfrontend.javac;
import pascal.taie.frontend.newfrontend.FrontendException;
import pascal.taie.project.ClassFile;
import pascal.taie.project.FileResource;
import pascal.taie.project.Resource;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class JavacSourceHandler {

    private final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    private final DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
    private final StandardJavaFileManager fileManager =
            compiler.getStandardFileManager(diagnostics, null, null);
    private final String tempOutDir = System.getProperty("java.io.tmpdir") + "/taie";

    public List<ClassFile> compile(String cp, String javaSourceFile, int javaVersion) throws IOException {
        if (compiler == null) {
            throw new FrontendException("No compiler available");
        }
        StringWriter output = new StringWriter();
        File javaFile = new File(javaSourceFile);
        Iterable<? extends JavaFileObject> compilationUnits =
                fileManager.getJavaFileObjectsFromFiles(List.of(javaFile));

        List<String> options = List.of("-classpath", cp, "-verbose",
                "--release", "" + javaVersion,
                "-d", tempOutDir,
                "-implicit:class");

        JavaCompiler.CompilationTask task =
                compiler.getTask(output, fileManager, diagnostics, options, null, compilationUnits);

        boolean success = task.call();

        if (!success) {
            StringBuilder errors = new StringBuilder();
            diagnostics.getDiagnostics().forEach(diagnostic -> {
                String sourceInfo = diagnostic.getSource() != null ?
                        diagnostic.getSource().toUri().toString() : "Unknown source";
                errors.append(String.format("Error on line %d in %s%n",
                        diagnostic.getLineNumber(),
                        sourceInfo));
                errors.append(String.format("Message: %s%n", diagnostic.getMessage(null)));
            });
            throw new FrontendException(
                    "Javac compilation failed for " + javaSourceFile + ":\n" + errors);
        }

        fileManager.close();
        List<ClassFile> compileResults = new ArrayList<>();
        for (String compileResult : getCompiledFiles(output.toString())) {
            compileResults.add(createPhantomClassFile(compileResult));
        }
        return compileResults;
    }

    private List<String> getCompiledFiles(String output) {
        String[] lines = output.split("\n");
        List<String> files = new ArrayList<>();
        for (String line : lines) {
            // TODO: check language & system settings affects output
            Pattern pattern = Pattern.compile("\\[wrote " + Pattern.quote(tempOutDir) + "/(.+)\\.class]");
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                String fileName = matcher.group(1);
                files.add(fileName);
            }
        }
        return files;
    }

    private ClassFile createPhantomClassFile(String internalName) throws IOException {
        Path p = Paths.get(tempOutDir, internalName + ".class");
        FileTime time = Files.getLastModifiedTime(p);
        Resource r = new FileResource(p);
        return new ClassFile(getName(internalName), internalName, time, r, null);
    }

    private String getName(String internalName) {
        int slashIndex = internalName.lastIndexOf('/');
        return (slashIndex == -1) ? internalName : internalName.substring(slashIndex + 1);
    }
}
