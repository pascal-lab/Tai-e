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

package pascal.taie.backend.bytecode;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.MethodTooLargeException;
import org.objectweb.asm.Opcodes;
import pascal.taie.Main;
import pascal.taie.World;
import pascal.taie.language.classes.JClass;

import javax.annotation.Nonnull;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * This class is responsible for processing a given JAR file by
 * loading its classes into Tai-e, converting them to Tai-e's representation,
 * then transforming them back into .class files, and finally packaging
 * these classes into a specified output JAR file.
 */
public class JarTransformer {

    private static final Logger logger = LogManager.getLogger(JarTransformer.class);

    /**
     * The main entry point for the JarTransformer.
     * This method takes three command-line arguments:
     * 1. the input JAR file path.
     * 2. the output JAR file path.
     * 3. the transformed Java version.
     */
    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: java JarTransformer <input-jar-file> <output-jar-file> <java-version>");
            return;
        }

        Path inputJar = Path.of(args[0]);
        Path outputJar = Path.of(args[1]);
        String javaVersion = args[2];
        int classFileVersion = getJavaVersion(javaVersion);

        if (!Files.exists(inputJar)) {
            throw new RuntimeException("Error: The file " + inputJar + " does not exist.");
        }

        // Load the input JAR into Tai-e
        Main.buildWorld("-pp", "-acp", inputJar.toString(), "--allow-phantom");
        List<JClass> classes = World.get().getClassHierarchy().allClasses().toList();
        try {
            Path tempDir = Files.createTempDirectory("jar-dumper");
            // Convert each JClass of the classes in the input JAR
            // to .class file and dump it to the output JAR
            for (JClass jClass : classes) {
                if (!jClass.isApplication()) {
                    continue;
                }
                try {
                    ClassFileDumper.dump(jClass, tempDir, classFileVersion);
                } catch (MethodTooLargeException e) {
                    logger.warn("Skip {}, the output method (name: {}) too large. " +
                                    "It's not an error, current implementation may encounter such case",
                            jClass, e.getMethodName());
                }
            }
            packJar(inputJar, tempDir, outputJar);
            deleteDir(tempDir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Packs the extracted classes into the output JAR file.
     *
     * @param inputJar  the input JAR file
     * @param tempDir   the temporary directory containing the extracted classes
     * @param outputJar the output JAR file
     * @throws IOException if an I/O error occurs
     */
    private static void packJar(Path inputJar, Path tempDir, Path outputJar)
            throws IOException {
        // Copy the original JAR to the new JAR
        Files.copy(inputJar, outputJar, StandardCopyOption.REPLACE_EXISTING);

        // Create a file system for the new JAR
        try (FileSystem jarFs = FileSystems.newFileSystem(outputJar)) {
            // Copy the files from the output directory to the JAR file system
            Files.walkFileTree(tempDir, new SimpleFileVisitor<>() {
                @Nonnull
                @Override
                public FileVisitResult visitFile(
                        @Nonnull Path file, @Nonnull BasicFileAttributes attrs)
                        throws IOException {
                    Path dest = jarFs.getPath(tempDir.relativize(file).toString());
                    if (!Files.exists(dest)) {
                        throw new FileNotFoundException("File " + dest +
                                " does not exist in the original JAR file," +
                                " may be dues to phantom classes");
                    } else {
                        Files.copy(file, dest, StandardCopyOption.REPLACE_EXISTING);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    /**
     * Deletes the temporary directory and its contents.
     *
     * @param dir the temporary directory to delete
     * @throws IOException if an I/O error occurs
     */
    private static void deleteDir(Path dir) throws IOException {
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder())
                    .forEach((f) -> {
                        try {
                            Files.delete(f);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
    }

    private static int getJavaVersion(String versionString) {
        if (versionString.equals("1.8")) {
            return Opcodes.V1_8;
        } else if (versionString.equals("17")) {
            return Opcodes.V17;
        } else {
            throw new IllegalArgumentException("Unsupported version: " + versionString);
        }
    }
}
