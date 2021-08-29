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
import pascal.taie.config.ConfigException;
import pascal.taie.config.Configs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Packs assignments according to configurations.
 */
final class AssignmentPacker {

    private static final String ASS_DIR = "assignments";

    private static final String TAI_E = "build/libs/tai-e-all.jar";

    private static final String SOURCE_DIR = "src/main/java";

    private static final File OUT_DIR = Configs.getOutputDir();

    public static void main(String[] args) {
//        try (JarInputStream in = new JarInputStream(new FileInputStream(TAI_E));
//             JarOutputStream out = new JarOutputStream(
//                     new FileOutputStream("output/out2.jar"),
//                     in.getManifest())
//        ) {
//            byte[] byteBuff = new byte[1024];
//            for (JarEntry entry; (entry = in.getNextJarEntry()) != null; ) {
//                System.out.println(entry);
//                if (entry.getName().startsWith("pascal")) {
//                    out.putNextEntry(entry);
//                    for (int bytesRead; (bytesRead = in.read(byteBuff)) != -1; ) {
//                        out.write(byteBuff, 0, bytesRead);
//                    }
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        Config config = Config.parseConfig(Path.of(ASS_DIR, "A1", "config.yml").toFile());
        config.toString();
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

        private boolean shouldIncludeClass(String item) {
            throw new UnsupportedOperationException();
        }

        private static String CLASS_SUFFIX = ".class";

        private boolean match(String item, String entry) {
            if (entry.endsWith("*")) { // entry is package pattern
                if (item.length() - CLASS_SUFFIX.length() > entry.length() - 1) {
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
    }
}
