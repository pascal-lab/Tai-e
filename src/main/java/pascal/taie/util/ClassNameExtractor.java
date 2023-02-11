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

import pascal.taie.util.collection.Lists;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * Utility class for extracting names of all classes inside
 * given JAR files or directories.
 */
public class ClassNameExtractor {

    private static final String JAR = ".jar";

    private static final String CLASS = ".class";

    private static final String JAVA = ".java";

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Options: <output-path> <path> <path> ...");
            System.out.println("<path> can be a path to a JAR file or" +
                    " a directory containing classes");
            return;
        }
        String outPath = args[0];
        System.out.printf("Dumping extracted class names to %s ...%n", outPath);
        String[] jars = Arrays.copyOfRange(args, 1, args.length);
        try (PrintStream out = new PrintStream(new FileOutputStream(outPath))) {
            for (String arg : jars) {
                extract(arg).forEach(out::println);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Extracts names of all classes in given path.
     */
    public static List<String> extract(String path) {
        return path.endsWith(JAR) ? extractJar(path) : extractDir(path);
    }

    private static List<String> extractJar(String jarPath) {
        try (JarFile jar = new JarFile(jarPath)) {
            System.out.printf("Scanning %s ... ", jarPath);
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
            throw new RuntimeException("Failed to read jar file: " + jarPath, e);
        }
    }

    private static List<String> extractDir(String dirPath) {
        Path dir = Paths.get(dirPath);
        if (!dir.toFile().isDirectory()) {
            throw new RuntimeException(dir + " is not a directory");
        }
        try (Stream<Path> paths = Files.walk(dir)) {
            System.out.printf("Scanning %s ... ", dirPath);
            List<String> classNames = new ArrayList<>();
            paths.map(dir::relativize).forEach(path -> {
                String fileName = path.getFileName().toString();
                int suffix;
                if (fileName.endsWith(CLASS)) {
                    suffix = CLASS.length();
                } else if (fileName.endsWith(JAVA)) {
                    suffix = JAVA.length();
                } else {
                    return;
                }
                String name = String.join(".",
                        Lists.map(Lists.asList(path), Path::toString));
                String className = name.substring(0, name.length() - suffix);
                classNames.add(className);
            });
            System.out.printf("%d classes%n", classNames.size());
            return classNames;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read directory: " + dirPath, e);
        }
    }
}
