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

package pascal.taie.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import static pascal.taie.util.PathUtils.CLASS;

/**
 * Utility class for extracting names of all classes inside
 * given JAR files or directories.
 */
public class ClassNameExtractor {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Options: <output-path> <path> <path> ...");
            System.out.println("<path> can be a path to a JAR file or" +
                    " a directory containing classes");
            return;
        }
        File outFile = new File(args[0]);
        System.out.printf("Dumping extracted class names to %s%n",
                outFile.getAbsolutePath());
        String[] paths = Arrays.copyOfRange(args, 1, args.length);
        try (PrintStream out = new PrintStream(new FileOutputStream(outFile))) {
            for (String path : paths) {
                extract(path).forEach(out::println);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Extracts names of all classes in given path.
     */
    public static List<String> extract(String path) {
        return extract(Path.of(path));
    }

    /**
     * Extracts names of all classes in given path.
     */
    private static List<String> extract(Path path) {
        return path.toFile().isDirectory() ? extractDir(path) : extractJar(path);
    }

    private static List<String> extractDir(Path dirPath) {
        try (Stream<Path> paths = Files.walk(dirPath)) {
            System.out.printf("Scanning %s ... ", dirPath.toAbsolutePath());
            List<String> classNames = new ArrayList<>();
            paths.map(dirPath::relativize).forEach(path -> {
                if (PathUtils.isJavaFile(path) || PathUtils.isClassFile(path)) {
                    classNames.add(PathUtils.toClassName(path));
                }
            });
            System.out.printf("%d classes%n", classNames.size());
            return classNames;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read directory: " + dirPath, e);
        }
    }

    private static List<String> extractJar(Path jarPath) {
        if (!PathUtils.isJarFile(jarPath)) {
            throw new RuntimeException(jarPath + " is not a JAR file");
        }
        File file = jarPath.toFile();
        try (JarFile jar = new JarFile(file)) {
            System.out.printf("Scanning %s ... ", file.getAbsolutePath());
            List<String> classNames = jar.stream()
                    .filter(e -> !e.getName().startsWith("META-INF"))
                    .filter(e -> e.getName().endsWith(CLASS))
                    .map(e -> {
                        String name = e.getName();
                        return name.replaceAll("/", ".")
                                .substring(0, name.length() - CLASS.length());
                    })
                    .toList();
            System.out.printf("%d classes%n", classNames.size());
            return classNames;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read JAR file: " +
                    file.getAbsolutePath(), e);
        }
    }
}
