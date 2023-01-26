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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarFile;

/**
 * Utility class for extracting names of all classes inside given jar files.
 */
public class ClassNameExtractor {

    private static final String CLASS_SUFFIX = ".class";

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Options: <output-path> <jar-path> <jar-path> ...");
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

    private static List<String> extract(String jarPath) {
        try (JarFile jar = new JarFile(jarPath)) {
            System.out.printf("Extracting %s ... ", jarPath);
            List<String> classNames = jar.stream()
                    .filter(e -> !e.getName().startsWith("META-INF"))
                    .filter(e -> e.getName().endsWith(CLASS_SUFFIX))
                    .map(e -> {
                        String name = e.getName();
                        return name.replaceAll("/", ".")
                                .substring(0, name.length() - CLASS_SUFFIX.length());
                    })
                    .toList();
            System.out.printf("%d class names%n", classNames.size());
            return classNames;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read jar file: " + jarPath, e);
        }
    }
}
