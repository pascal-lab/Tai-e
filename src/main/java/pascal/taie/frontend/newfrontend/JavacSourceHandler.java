/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2022 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2022 Yue Li <yueli@nju.edu.cn>
 *
 * This file is part of Tai-e.
 *
 * Tai-e is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Tai-e is distributed in the hope that it will be useful,but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Tai-e. If not, see <https://www.gnu.org/licenses/>.
 */

package pascal.taie.frontend.newfrontend;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.frontend.newfrontend.exception.FrontendException;
import pascal.taie.frontend.newfrontend.exception.JavacException;
import pascal.taie.project.DotClassFile;
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
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


// TODO: check concurrent behavior, note compile may be called concurrently

/**
 * This class handles the compilation of Java source files using the Java Compiler API.
 * It provides methods to compile Java files, collect diagnostics, and manage temporary output directories.
 */
public class JavacSourceHandler {

    private final Logger logger = LogManager.getLogger(JavacSourceHandler.class);

    // ---------------- JAVA COMPILER RELATED (starts) ----------------
    private final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

    private final DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

    private final StandardJavaFileManager fileManager =
            compiler.getStandardFileManager(diagnostics, null, null);
    // ---------------- JAVA COMPILER RELATED (ends) ----------------

    /**
     * Temporary output directory for compiled class files.
     */
    private final Path tempOutDir = Path.of(System.getProperty("java.io.tmpdir")).resolve("taie");

    /**
     * Regex pattern to extract the name of the compiled class file from the compiler output.
     */
    private final Pattern writePattern = getWritePattern();

    public List<DotClassFile> compile(String cp, String javaSourceFile, int javaVersion)
            throws JavacException, IOException {
        if (compiler == null) {
            throw new JavacException();
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
            throw new JavacException(
                    "Javac compilation failed for " + javaSourceFile + ":\n" + errors);
        }

        fileManager.close();
        List<DotClassFile> compileResults = new ArrayList<>();
        for (String compileResult : getCompiledFiles(output.toString())) {
            compileResults.add(createPhantomClassFile(compileResult));
        }
        if (compileResults.isEmpty()) {
            throw new JavacException(
                    String.format("""
                    Javac compilation failed for %s. Insufficient information was found to determine the cause.
                    Please check the following potential reasons:
                    1) Ensure JAVA_TOOL_OPTIONS is properly set. Refer to the warning message for guidance.
                    2) Verify that your JDK version meets the minimum requirement of Java 11. We recommend using Java 17 or higher.
                    3) This might be a Tai-e bug, consider submit a bug report at %s""", javaSourceFile, FrontendException.TAIE_ISSUES));
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

    private DotClassFile createPhantomClassFile(String outputPath) throws IOException {
        Path output = Path.of(outputPath);
        Path relative = tempOutDir.relativize(output);
        String className = PathUtils.getClassName(relative);
        String internalName = PathUtils.getInternalName(relative);
        FileTime time = Files.getLastModifiedTime(output);
        Resource r = new FileResource(output);
        return new DotClassFile(className, internalName, time, r, null);
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
            logger.warn("""
                    Unknown language: {}, country: {}
                    The Java source code frontend may not work properly
                    Suggest add env: [JAVA_TOOL_OPTIONS=-Duser.language=en]""", lang, country);
            return Pattern.compile("\\[wrote (.*\\.class)]");
        }
    }
}
