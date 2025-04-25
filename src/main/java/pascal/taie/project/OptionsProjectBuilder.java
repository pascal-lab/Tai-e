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

package pascal.taie.project;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.config.Options;
import pascal.taie.util.ClassNameExtractor;
import pascal.taie.util.collection.Streams;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class OptionsProjectBuilder implements ProjectBuilder {

    private static final String JREs = "java-benchmarks/JREs";
    private static final Logger logger = LogManager.getLogger(OptionsProjectBuilder.class);
    private static final String JRE_FIND_FAILED = """
            Failed to locate Java library.
            Please clone submodule 'java-benchmarks' by command:
            git submodule update --init --recursive
            and put it in Tai-e's working directory.""";
    private final Options options;

    private Project project;

    public OptionsProjectBuilder(Options options) {
        this.options = options;
    }

    /**
     * Obtains all input classes specified in {@code options}.
     */
    private static List<String> getInputClasses(Options options) {
        List<String> classes = new ArrayList<>();
        // process --input-classes
        options.getInputClasses().forEach(value -> {
            if (value.endsWith(".txt")) {
                // value is a path to a file that contains class names
                try (Stream<String> lines = Files.lines(Path.of(value))) {
                    lines.forEach(classes::add);
                } catch (IOException e) {
                    logger.warn("Failed to read input class file {} due to {}",
                            value, e);
                }
            } else {
                // value is a class name
                classes.add(value);
            }
        });
        // process --app-class-path
        List<String> appClassPath = options.getAppClassPath();
        for (String path : appClassPath) {
            classes.addAll(ClassNameExtractor.extract(path));
        }
        return classes;
    }

    private String getMainClass() {
        return options.getMainClass();
    }

    private int getJavaVersion() {
        return options.getJavaVersion();
    }

    private List<String> getInputClasses() {
        return getInputClasses(options);
    }

    private List<FileContainer> getAppContainers(List<String> appClassPaths) throws IOException {
        return FileLoader.get().loadRootContainers(
                appClassPaths.stream().distinct().map(Path::of).toList());
    }

    private List<FileContainer> getLibContainers(List<String> libClassPaths,
                                                 @Nullable String jrePath,
                                                 boolean isPrependJVM,
                                                 int javaVersion) throws IOException {
        List<FileContainer> libs = FileLoader.get().loadRootContainers(
                libClassPaths.stream().distinct().map(Path::of).toList());
        // add jre
        List<FileContainer> jre = getJREContainers(jrePath, isPrependJVM, javaVersion);
        return Streams.concat(libs.stream(), jre.stream()).toList();
    }

    private List<FileContainer> getJREContainers(@Nullable String jrePath, boolean isPrependJVM, int javaVersion) throws IOException {
        if (isPrependJVM) {
            // if prependJVM is set, we use jrt:/ to load JRE
            FileSystem fs = FileSystems.getFileSystem(URI.create("jrt:/"));
            return processModulesFile(fs.getPath("/modules"));
        } else if (jrePath == null) {
            // if jrePath is not set, we use java-benchmarks to load JRE
            return getJREFromJavaBenchmarks(javaVersion);
        } else {
            // otherwise, load jre from the specified path
            return parseJREPath(jrePath);
        }
    }

    /**
     * Parse the given JRE path and return a list of FileContainer.
     * <p>
     * The JRE path should be one of the following:
     * <ul>
     *     <li>A {@code JAVA_HOME} like directory. For java 9 and above,
     *     it should contain a {@code lib/modules} and {@code lib/jrt-fs.jar} file. For java 8 and below,
     *     it should contain a {@code jre/lib} directory.</li>
     *     <li>A directory with {@code rt.jar} and related jars.</li>
     *     <li>A directory with {@code modules} file and {@code jrt-fs.jar} file.</li>
     * </ul>
     * </p>
     * @throws IOException when the JRE path is invalid or cannot be read.
     */
    private List<FileContainer> parseJREPath(String jrePath) throws IOException {
        Path p = Path.of(jrePath);
        if (!Files.exists(p) || !Files.isDirectory(p)) {
            throw new IOException(String.format("%s (--jre-dir) not found or not a directory", jrePath));
        }
        if (Files.exists(p.resolve("modules")) && Files.exists(p.resolve("jrt-fs.jar"))) {
            // try to parse with modules file
            return processModulesFile(p.resolve("modules"), p.resolve("jrt-fs.jar"));
        } else if (Files.exists(p.resolve("rt.jar"))) {
            // try to parse with rt.jar
            return processJarDirectory(p);
        } else if (Files.exists(p.resolve("jre/lib"))) {
            // try to parse with jre/lib
            return processJarDirectory(p.resolve("jre/lib"));
        } else if (Files.exists(p.resolve("lib/modules"))
                && Files.exists(p.resolve("lib/jrt-fs.jar"))) {
            // try to parse with lib/modules
            return processModulesFile(p.resolve("lib/modules"), p.resolve("lib/jrt-fs.jar"));
        } else {
            throw new IOException(String.format(
                    """
                    We don't know how to read %s (--jre-dir)
                    It must be a directory with one of the following:
                    1. A JAVA_HOME like directory. For java 9 and above,
                       it should contain a lib/modules and lib/jrt-fs.jar file.
                       For java 8 and below, it should contain a jre/lib directory.
                    2. A directory with rt.jar and related jars.
                    3. A directory with modules file.
                    """, jrePath));
        }
    }

    private List<FileContainer> processModulesFile(Path modules, Path jrtfs) throws IOException {
        FileSystem fs = FileSystemManager.get().getJrtFs(modules, jrtfs);
        return processModulesFile(fs.getPath("modules"));
    }

    private List<FileContainer> processModulesFile(Path modules) throws IOException {
        try (Stream<Path> paths = Files.list(modules)) {
            return FileLoader.get().loadRootContainers(paths.toList());
        }
    }

    private List<FileContainer> processJarDirectory(Path jarDir) throws IOException {
        try (Stream<Path> paths = Files.list(jarDir)) {
            return FileLoader.get().loadRootContainers(
                    paths.filter(p -> p.toString().endsWith(".jar"))
                            .distinct()
                            .toList());
        }
    }

    private List<FileContainer> getJREFromJavaBenchmarks(int javaVersion) throws IOException {
        File jreDir = new File(JREs);
        if (!jreDir.exists()) {
            throw new IOException(JRE_FIND_FAILED);
        }
        String jrePath = String.format("%s/jre" + ((javaVersion <= 8) ? "1.%d" : "%d"),
                JREs, javaVersion);
        Path jarDir = Path.of(jrePath);
        if (!Files.exists(jarDir)) {
            throw new IOException("JRE not found: " + jrePath + "\n" +
                    "If you have not fully clone the submodule 'java-benchmarks', please clone it first.\n" +
                    "Otherwise, we do not include JRE of java " + javaVersion + " in 'java-benchmarks'\n" +
                    "Please specify the path to your JRE by option --jre-dir");
        }
        return processJarDirectory(jarDir);
    }

    @Override
    public Project build() {
        try {
            List<String> appClassPaths = options.getAppClassPath();

            List<String> libClassPaths = new ArrayList<>(options.getClassPath());
            libClassPaths.removeAll(appClassPaths);

            project = new Project(
                    getMainClass(),
                    getJavaVersion(),
                    getInputClasses(),
                    getAppContainers(appClassPaths),
                    getLibContainers(libClassPaths, options.getJreDir(),
                            options.isPrependJVM(), options.getJavaVersion()),
                    String.join(File.pathSeparator,
                            Stream.concat(
                                    options.getClassPath().stream(),
                                    options.getAppClassPath().stream()).toList()));
            return project;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
