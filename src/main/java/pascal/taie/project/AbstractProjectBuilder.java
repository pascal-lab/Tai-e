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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AbstractProjectBuilder implements ProjectBuilder {

    private static final Logger logger = LogManager.getLogger(AbstractProjectBuilder.class);

    protected static final String JREs = "java-benchmarks/JREs";
    private static final String JRE_FIND_FAILED = """
            Failed to locate Java library.
            Please clone submodule 'java-benchmarks' by command:
            git submodule update --init --recursive
            and put it in Tai-e's working directory.""";

    protected abstract String getMainClass();

    protected abstract int getJavaVersion();

    protected abstract List<String> getInputClasses();

    protected abstract List<FileContainer> getRootContainers();

    @Override
    public Project build() {
        return new Project(getMainClass(), getJavaVersion(),
                getInputClasses(), getRootContainers(), null);
    }

    /**
     * return value excludes app-class-path
     */
    protected static String getClassPath(Options options) {
        if (options.isPrependJVM()) {
            return options.getClassPath();
        } else if (options.getJreDir() != null || options.getJavaVersion() >= 8) {
            // use another method for jre path
            return options.getClassPath();
        } else { // when prependJVM is not set, we manually specify JRE jars
            // check existence of JREs
            File jreDir = new File(JREs);
            if (!jreDir.exists()) {
                throw new RuntimeException(JRE_FIND_FAILED);
            }
            String jrePath = String.format("%s/jre1.%d",
                    JREs, options.getJavaVersion());
            try (Stream<Path> paths = Files.walk(Path.of(jrePath))) {
                return Streams.concat(
                                paths.map(Path::toString).filter(p -> p.endsWith(".jar")),
                                Stream.ofNullable(options.getClassPath()))
                        .collect(Collectors.joining(File.pathSeparator));
            } catch (IOException e) {
                throw new RuntimeException("Analysis on Java " +
                        options.getJavaVersion() + " library is not supported yet", e);
            }
        }
    }

    /**
     * Obtains all input classes specified in {@code options}.
     */
    protected static List<String> getInputClasses(Options options) {
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
        String appClassPath = options.getAppClassPath();
        if (appClassPath != null) {
            for (String path : appClassPath.split(File.pathSeparator)) {
                classes.addAll(ClassNameExtractor.extract(path));
            }
        }
        return classes;
    }

    protected static Stream<Path> listJrtModule(Options options) throws IOException {
        int javaVersion = options.getJavaVersion();
        if (javaVersion <= 8) {
            return Stream.empty();
        }

        FileSystem fs;
        if (!options.isPrependJVM()) {
            Path jreDir;
            if (options.getJreDir() != null) {
                jreDir = Path.of(options.getJreDir());
            } else if (javaVersion == 11 || javaVersion == 17) {
                Path jreFolder = Path.of(JREs, "jre" + javaVersion);
                if (Files.isDirectory(jreFolder)) {
                    jreDir = jreFolder;
                } else {
                    throw new RuntimeException(JRE_FIND_FAILED);
                }
            } else {
                // TODO: produce error, JRE may not loaded
                return Stream.empty();
            }
            fs = FSManager.get().getJrtFs(jreDir);
        } else {
            fs = FileSystems.getFileSystem(URI.create("jrt:/"));
        }
        Path modulePath = fs.getPath("/modules");
        return Files.list(modulePath);
    }
}
