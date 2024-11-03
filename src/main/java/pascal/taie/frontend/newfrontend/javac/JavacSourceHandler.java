package pascal.taie.frontend.newfrontend.javac;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.frontend.newfrontend.exception.FrontendException;
import pascal.taie.project.ClassFile;
import pascal.taie.project.FileResource;
import pascal.taie.project.PathUtils;
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


// TODO: check concurrent behavior, note compile may be called concurrently
public class JavacSourceHandler {

    private final Logger logger = LogManager.getLogger(JavacSourceHandler.class);
    private final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    private final DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
    private final StandardJavaFileManager fileManager =
            compiler.getStandardFileManager(diagnostics, null, null);
    private final Path tempOutDir = Path.of(System.getProperty("java.io.tmpdir")).resolve("taie");
    private final Pattern writePattern = getWritePattern();

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
                "-d", tempOutDir.toAbsolutePath().toString(),
                "-implicit:class",
                "-g");

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
            Matcher matcher = writePattern.matcher(line);
            if (matcher.find()) {
                String fileName = matcher.group(1);
                files.add(fileName);
            }
        }
        return files;
    }

    private ClassFile createPhantomClassFile(String outputPath) throws IOException {
        Path output = Paths.get(outputPath);
        Path relative = tempOutDir.relativize(output);
        String className = PathUtils.getClassName(relative);
        String internalName = PathUtils.getInternalName(relative);
        FileTime time = Files.getLastModifiedTime(output);
        Resource r = new FileResource(output);
        return new ClassFile(className, internalName, time, r, null);
    }

    private Pattern getWritePattern() {
        String lang = System.getProperty("user.language");
        String country = System.getProperty("user.country");
        if (lang.equals("en")) {
            return Pattern.compile("\\[wrote (.*\\.class)]");
        } else if (lang.equals("de")) {
            return Pattern.compile("\\[(.*\\.class) geschrieben]");
        } else if (lang.equals("ja")) {
            return Pattern.compile("\\[(.*\\.class)を書込み完了]");
        } else if (lang.equals("zh") && country.equals("CN")) {
            return Pattern.compile("\\[已写入(.*\\.class)]");
        } else {
            logger.warn("Unknown language: {}, country: {}\n" +
                    "The Java source code frontend may not work properly\n" +
                    "Suggest add env: [JAVA_TOOL_OPTIONS=-Duser.language=en]", lang, country);
            return Pattern.compile("\\[wrote (.*\\.class)]");
        }
    }
}
