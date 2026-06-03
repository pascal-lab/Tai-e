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

package pascal.taie.frontend.java.closedworld;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pascal.taie.frontend.java.FrontendException;
import pascal.taie.frontend.java.project.DotClassFile;
import pascal.taie.frontend.java.project.FileResource;
import pascal.taie.frontend.java.project.Resource;
import pascal.taie.util.PathUtils;

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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class handles Java source files using the Java Compiler API.
 * It provides methods to compile Java source files, collect diagnostics,
 * and manage temporary output directories.
 */
class JavacSourceHandler {

    private static final Logger logger = LoggerFactory.getLogger(JavacSourceHandler.class);

    /**
     * Temporary output directory for compiled class files.
     */
    private static final Path tempOutRoot = Path.of(System.getProperty("java.io.tmpdir"))
            .resolve("tai-e");

    /**
     * Regex pattern to extract the name of the compiled class file from the compiler output.
     */
    private static final Pattern writePattern = getWritePattern();

    public static List<DotClassFile> compile(String cp, String javaSourceFile, int javaVersion)
            throws FrontendException, IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new FrontendException("Failed to process .java file " +
                    "since javac instance cannot be obtained. " +
                    "Please ensure that the JDK is properly configured.");
        }
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(
                diagnostics, null, null);

        StringWriter output = new StringWriter();
        File javaFile = new File(javaSourceFile);
        Iterable<? extends JavaFileObject> compilationUnits =
                fileManager.getJavaFileObjectsFromFiles(List.of(javaFile));

        Files.createDirectories(tempOutRoot);
        Path tempOutDir = Files.createTempDirectory(tempOutRoot, "javac-");
        List<String> options = List.of("-classpath", cp,
                "-d", tempOutDir.toAbsolutePath().toString(),
                "--release", Integer.toString(javaVersion),
                "-implicit:class", "-g", "-verbose");

        JavaCompiler.CompilationTask task = compiler.getTask(output, fileManager,
                diagnostics, options, null, compilationUnits);
        if (!task.call()) {
            StringBuilder errors = new StringBuilder();
            diagnostics.getDiagnostics().forEach(diagnostic -> {
                String sourceInfo = (diagnostic.getSource() != null) ?
                        diagnostic.getSource().toUri().toString() : "Unknown source";
                errors.append(String.format("Error on line %d in %s%n",
                        diagnostic.getLineNumber(),
                        sourceInfo));
                errors.append(String.format("Message: %s%n", diagnostic.getMessage(null)));
            });
            throw new FrontendException(
                    "Failed to process .java file due to javac compilation errors for "
                            + javaSourceFile + ":\n" + errors);
        }
        fileManager.close();

        List<DotClassFile> compileResults = getCompiledFiles(output.toString())
                .stream()
                .map(path -> createCompiledClassFile(tempOutDir, path))
                .toList();
        if (compileResults.isEmpty()) {
            throw new FrontendException(
                    String.format("""
                                    Failed to process .java file due to javac compilation errors for %s.
                                    Insufficient information was found to determine the cause.
                                    Please check the following potential reasons:
                                    1) Ensure JAVA_TOOL_OPTIONS is properly set. Refer to the warning message for guidance.
                                    2) Verify that your JDK version meets the requirement: Java 17 or higher.
                                    3) This might be a Tai-e bug, consider submit a bug report at %s""",
                            javaSourceFile, FrontendException.TAIE_ISSUES));
        }
        return compileResults;
    }

    private static List<String> getCompiledFiles(String output) {
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

    private static DotClassFile createCompiledClassFile(Path tempOutDir, String outputPath) {
        Path output = Path.of(outputPath);
        Path relative = tempOutDir.relativize(output);
        String className = PathUtils.toClassName(relative);
        Resource resource = new FileResource(output);
        return new DotClassFile(className, resource, null);
    }

    private static Pattern getWritePattern() {
        String lang = System.getProperty("user.language");
        return switch (lang) {
            case "en" -> Pattern.compile("\\[wrote (.*\\.class)]");
            // TODO: take country/region code (zh-CN/zh-HK/zh-Tw) into account?
            case "zh" -> Pattern.compile("\\[已写入(.*\\.class)]");
            case "de" -> Pattern.compile("\\[(.*\\.class) geschrieben]");
            case "ja" -> Pattern.compile("\\[(.*\\.class)を書込み完了]");
            default -> {
                logger.warn("""
                        Unknown language: {}
                        JavacSourceHandler may not work properly with javac.
                        Possible fix: add 'JAVA_TOOL_OPTIONS=-Duser.language=en' to env""", lang);
                yield Pattern.compile("\\[wrote (.*\\.class)]");
            }
        };
    }
}
