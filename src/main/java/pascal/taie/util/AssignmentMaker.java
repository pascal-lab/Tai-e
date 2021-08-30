/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2020-- Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020-- Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * Tai-e is only for educational and academic purposes,
 * and any form of commercial use is disallowed.
 * Distribution of Tai-e is disallowed without the approval.
 */

package pascal.taie.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.config.ConfigException;
import pascal.taie.config.Configs;
import pascal.taie.util.collection.Streams;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;

/**
 * Makes assignments according to configurations.
 */
final class AssignmentMaker {

    private static final Logger logger = LogManager.getLogger(AssignmentMaker.class);

    /**
     * Relative path to Tai-e jar.
     */
    private static final String TAI_E = "build/libs/tai-e-all.jar";

    private static final Path SOURCE_DIR = Path.of("src/main/java");

    private static final Path TEST_SOURCE_DIR = Path.of("src/test/java");

    private static final Path TEST_RESOURCES_DIR = Path.of("src/test/resources");

    private final static String CLASS_SUFFIX = ".class";

    public static void main(String[] args) {
        for (String a : args) {
            new AssignmentMaker(a).make();
        }
    }

    /**
     * Path to the directory for assignment content.
     */
    private final Path ASS_DIR;

    /**
     * Path to the directory for the generated assignment.
     */
    private final Path TARGET_DIR;

    private final Config config;

    private AssignmentMaker(String name) {
        ASS_DIR = Path.of("assignments", name);
        TARGET_DIR = Configs.getOutputDir().toPath().resolve(name);
        File target = TARGET_DIR.toFile();
        if (target.exists()) {
            deleteDirectory(TARGET_DIR);
        }
        target.mkdirs();
        config = Config.parseConfig(ASS_DIR.resolve("config.yml").toFile());
    }

    /**
     * Deletes a directory and all content in it.
     *
     * @param dir the path of the directory to be deleted.
     */
    private static void deleteDirectory(Path dir) {
        try {
            Files.walk(dir)
                    .map(Path::toFile)
                    .sorted(Comparator.reverseOrder())
                    .forEach(File::delete);
        } catch (IOException e) {
            logger.warn("Exception {} thrown when deleting {}", e, dir);
        }
    }

    private void make() {
        logger.info("Making assignment {} at {} ...",
                config.getName(), TARGET_DIR);
        packDependencies();
        copySourceFiles();
        copyIncompleteFiles();
        copyTestClasses();
        copyTestResources();
    }

    private void packDependencies() {
        Path path = TARGET_DIR.resolve("lib/dependencies.jar");
        File parent = path.getParent().toFile();
        if (!parent.exists()) {
            parent.mkdirs();
        }
        try (var in = new JarInputStream(new FileInputStream(TAI_E));
             var out = new JarOutputStream(new FileOutputStream(path.toFile()))
        ) {
            byte[] byteBuff = new byte[1024];
            for (JarEntry entry; (entry = in.getNextJarEntry()) != null; ) {
                String entryName = entry.getName();
                if (!entryName.endsWith(CLASS_SUFFIX) ||
                        config.shouldIncludeClass(entry.getName())) {
                    out.putNextEntry(entry);
                    for (int bytesRead; (bytesRead = in.read(byteBuff)) != -1; ) {
                        out.write(byteBuff, 0, bytesRead);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to pack dependencies", e);
        }
    }

    private void copySourceFiles() {
        Path target = TARGET_DIR.resolve(SOURCE_DIR);
        config.getSourceFiles()
                .forEach(className -> copyClass(SOURCE_DIR, target, className,
                        AssignmentMaker::toSourcePath));
    }

    private void copyIncompleteFiles() {
        Path source = ASS_DIR;
        Path target = TARGET_DIR.resolve(SOURCE_DIR);
        config.getIncompleteFiles()
                .forEach(className -> copyClass(source, target, className,
                        AssignmentMaker::toSourcePath));
    }

    private void copyTestClasses() {
        Path target = TARGET_DIR.resolve(TEST_SOURCE_DIR);
        config.getTestClasses()
                .forEach(className -> copyClass(TEST_SOURCE_DIR, target, className,
                        AssignmentMaker::toSourcePath));
    }

    private void copyClass(Path source, Path target,
                           String item, Function<String, String> converter) {
        String path = converter.apply(item);
        Path from = source.resolve(path);
        Path to = target.resolve(path);
        File parent = to.getParent().toFile();
        if (!parent.exists()) {
            parent.mkdirs();
        }
        try {
            Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to copy " + from + " to " + to, e);
        }
    }

    /**
     * Converts a class name to corresponding path of source file,
     * e.g., class a.b.C will be converted to a/b/C.java.
     */
    private static String toSourcePath(String className) {
        String[] split = className.split("\\.");
        split[split.length - 1] = split[split.length - 1] + ".java";
        return String.join(File.separator, Arrays.asList(split));
    }

    private void copyTestResources() {
        Path target = TARGET_DIR.resolve(TEST_RESOURCES_DIR);
        config.getTestResources().forEach(item -> {
            copyClass(TEST_RESOURCES_DIR, target, item, Function.identity());
            copyClass(TEST_RESOURCES_DIR, target, item,
                    AssignmentMaker::toExpectedPath);
        });
    }

    private static @Nullable
    String toExpectedPath(String testPath) {
        String[] split = testPath.split("/");
        String file = split[split.length - 1];
        if (file.endsWith(".java")) {
            split[split.length - 1] = file.substring(0,
                    file.length() - ".java".length()) + "-expected.txt";
            return String.join(File.separator, Arrays.asList(split));
        }
        return null;
    }

    /**
     * Configuration for which files should be included in each assignment.
     */
    private static class Config {

        /**
         * Name of this assignment, which should be "Ax" where x is
         * the assignment number.
         */
        private final String name;

        /**
         * Classes to be excluded in the assignment.
         */
        private final List<String> exclude;

        /**
         * Source files to be included in the assignment.
         */
        private final List<String> sourceFiles;

        /**
         * Incomplete files to be included in the assignment.
         * These files should have been prepared in folder Ax/.
         */
        private final List<String> incompleteFiles;

        /**
         * Test classes to be included in the assignment.
         */
        private final List<String> testClasses;

        /**
         * Test resources (i.e., test-related files) to be included in the assignment.
         */
        private final List<String> testResources;

        @JsonCreator
        private Config(
                @JsonProperty("name") String name,
                @JsonProperty("exclude") List<String> exclude,
                @JsonProperty("sourceFiles") List<String> sourceFiles,
                @JsonProperty("incompleteFiles") List<String> incompleteFiles,
                @JsonProperty("testClasses") List<String> testClasses,
                @JsonProperty("testResources") List<String> testResources) {
            this.name = name;
            this.exclude = Objects.requireNonNullElse(exclude, List.of());
            this.sourceFiles = Objects.requireNonNullElse(sourceFiles, List.of());
            this.incompleteFiles = Objects.requireNonNull(incompleteFiles);
            this.testClasses = Objects.requireNonNullElse(testClasses, List.of());
            this.testResources = Objects.requireNonNullElse(testResources, List.of());
        }

        private String getName() {
            return name;
        }

        private List<String> getSourceFiles() {
            return sourceFiles;
        }

        private List<String> getIncompleteFiles() {
            return incompleteFiles;
        }

        private List<String> getTestClasses() {
            return testClasses;
        }

        private List<String> getTestResources() {
            return testResources;
        }

        private static Config parseConfig(File configFile) {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            try {
                return mapper.readValue(configFile, Config.class);
            } catch (IOException e) {
                throw new ConfigException("Failed to read assignment config " + configFile);
            }
        }

        private boolean shouldIncludeClass(String item) {
            return Streams.concat(exclude.stream(),
                            sourceFiles.stream(),
                            incompleteFiles.stream())
                    .noneMatch(entry -> match(item, entry));
        }

        private boolean match(String item, String entry) {
            if (entry.endsWith("*")) { // entry is package pattern
                if (item.length() - CLASS_SUFFIX.length() < entry.length() - 1) {
                    return false;
                }
                return matchFirstN(item, entry, entry.length() - 1);
            } else { // entry is exact class name
                if (item.length() - CLASS_SUFFIX.length() != entry.length()) {
                    return false;
                }
                return matchFirstN(item, entry, entry.length());
            }
        }

        private static boolean matchFirstN(String item, String entry, int n) {
            for (int i = 0; i < n; ++i) {
                if (!match(item.charAt(i), entry.charAt(i))) {
                    return false;
                }
            }
            return true;
        }

        private static boolean match(char ic, char ec) {
            if (ic == '/') {
                return ec == '.';
            } else {
                return ic == ec;
            }
        }
    }
}
