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

package pascal.taie.analysis.pta.plugin.spring.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class DirectoryTraverser {

    private static final Logger logger = LoggerFactory.getLogger(DirectoryTraverser.class);

    private static final String CLASS_EXT = ".class";

    private static final String JAVA_EXT = ".java";

    static final String JAR_EXT = ".jar";

    private DirectoryTraverser() {
    }

    static void walkDirectory(String directoryPath,
                              boolean includeJar,
                              Predicate<String> fileNamePredicate,
                              BiConsumer<String, InputStream> action) {
        Objects.requireNonNull(fileNamePredicate, "fileNamePredicate cannot be null");
        Objects.requireNonNull(action, "action cannot be null");
        Path dirPath = Path.of(directoryPath);
        if (!Files.isDirectory(dirPath)) {
            throw new IllegalArgumentException("Path is not a directory: " + directoryPath);
        }
        try (var paths = Files.walk(dirPath)) {
            paths.filter(Files::isRegularFile)
                    .forEach(filePath -> processPath(
                            filePath, includeJar, fileNamePredicate, action));
        } catch (IOException e) {
            logger.error("Failed to walk directory: {}", directoryPath, e);
            throw new UncheckedIOException("Directory traversal failed", e);
        }
    }

    private static void processPath(Path filePath,
                                    boolean includeJar,
                                    Predicate<String> fileNamePredicate,
                                    BiConsumer<String, InputStream> action) {
        String filePathStr = filePath.toString();
        if (fileNamePredicate.test(filePathStr)) {
            try (InputStream inputStream = Files.newInputStream(filePath)) {
                action.accept(filePathStr, inputStream);
            } catch (IOException e) {
                logger.error("Failed to process file: {}", filePath, e);
            }
        }
        if (includeJar && filePathStr.endsWith(JAR_EXT)) {
            walkJarFile(filePath.toAbsolutePath().toString(), fileNamePredicate, action);
        }
    }

    static void walkJarFile(String jarFilePath,
                            Predicate<String> fileNamePredicate,
                            BiConsumer<String, InputStream> action) {
        Objects.requireNonNull(fileNamePredicate, "fileNamePredicate cannot be null");
        Objects.requireNonNull(action, "action cannot be null");
        try (JarFile jarFile = new JarFile(new File(jarFilePath))) {
            jarFile.stream()
                    .filter(entry -> !entry.isDirectory())
                    .filter(entry -> fileNamePredicate.test(entry.getName()))
                    .forEach(entry -> processJarEntry(jarFile, entry, action));
        } catch (IOException e) {
            logger.error("Failed to process JAR file: {}", jarFilePath, e);
            throw new UncheckedIOException("JAR file processing failed", e);
        }
    }

    private static void processJarEntry(JarFile jarFile,
                                        JarEntry entry,
                                        BiConsumer<String, InputStream> action) {
        try (InputStream inputStream = jarFile.getInputStream(entry)) {
            action.accept(entry.getName(), inputStream);
        } catch (IOException e) {
            logger.error("Failed to process JAR entry: {}", entry.getName(), e);
        }
    }

    public static List<String> listClasses(String path) {
        return path.endsWith(JAR_EXT)
                ? listClassesInJar(path)
                : listClassesInDir(path);
    }

    private static List<String> listClassesInJar(String jarFilePath) {
        try (JarFile jarFile = new JarFile(new File(jarFilePath))) {
            return jarFile.stream()
                    .filter(entry -> !entry.isDirectory())
                    .map(JarEntry::getName)
                    .filter(name -> name.endsWith(CLASS_EXT))
                    .map(DirectoryTraverser::normalizeClassName)
                    .toList();
        } catch (IOException e) {
            logger.error("Failed to list classes in JAR: {}", jarFilePath, e);
            throw new UncheckedIOException("Failed to process JAR file", e);
        }
    }

    private static List<String> listClassesInDir(String directoryPath) {
        Path dirPath = Path.of(directoryPath);
        String basePath = dirPath + File.separator;
        try (var paths = Files.walk(dirPath)) {
            return paths.filter(Files::isRegularFile)
                    .map(Path::toString)
                    .filter(path -> path.endsWith(CLASS_EXT) || path.endsWith(JAVA_EXT))
                    .map(path -> normalizeClassName(path.replace(basePath, "")))
                    .toList();
        } catch (IOException e) {
            logger.error("Failed to list classes in directory: {}", directoryPath, e);
            throw new UncheckedIOException("Failed to list classes", e);
        }
    }

    private static String normalizeClassName(String classPath) {
        return removeSuffix(removeSuffix(classPath, CLASS_EXT), JAVA_EXT)
                .replace(File.separator, ".")
                .replace("/", ".");
    }

    private static String removeSuffix(String string, String suffix) {
        return string.endsWith(suffix)
                ? string.substring(0, string.length() - suffix.length())
                : string;
    }
}
