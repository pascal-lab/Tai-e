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

package pascal.taie.dumpjvm;

import pascal.taie.Main;
import pascal.taie.World;
import pascal.taie.analysis.misc.IRDumper;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.language.classes.JClass;

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

public class JarDumper {
    public static void main(String[] args) {
        Path p = Path.of(args[0]);
        Path newJar = Path.of(args[1]);
        if (!Files.exists(p)) {
            throw new RuntimeException("Error: The file " + p + " does not exist.");
        }
        Main.buildWorld("-pp", "-acp", p.toString(),
                "--world-builder", "pascal.taie.frontend.newfrontend.AsmWorldBuilder",
                "--allow-phantom");
        // convert each class in this file
        List<JClass> classes = World.get().getClassHierarchy().allClasses().toList();
        IRDumper dumper = new IRDumper(AnalysisConfig.of(IRDumper.ID));
        JClass klass = World.get().getClassHierarchy().getClass("clojure.lang.WarnBoxedMath");
        dumper.analyze(klass);
        try {
            Path tempDir = Files.createTempDirectory("jar-dumper");
            for (JClass jClass : classes) {
                if (!jClass.isApplication()) { continue; }
                try {
                    byte[] classfileBuffer = new BytecodeEmitter().emit(jClass);
                    Path classfilePath = tempDir.resolve(
                            BytecodeEmitter.computeInternalName(jClass) + ".class");
                    Files.createDirectories(classfilePath.getParent());
                    Files.write(classfilePath, classfileBuffer);
                } catch (Exception e) {
                    System.out.println("Exception while write " + jClass);
                }
            }
            packJar(p, tempDir, newJar);
            deleteDir(tempDir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private static void packJar(Path oldJar, Path outputDir, Path newJar) throws IOException {
        // Copy the original JAR to the new JAR
        Files.copy(oldJar, newJar, StandardCopyOption.REPLACE_EXISTING);

        // Create a file system for the new JAR
        try (FileSystem jarFs = FileSystems.newFileSystem(newJar)) {
            // Copy the files from the output directory to the JAR file system
            Files.walkFileTree(outputDir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path dest = jarFs.getPath(outputDir.relativize(file).toString());
                    if (!Files.exists(dest)) {
                        throw new FileNotFoundException("File " + dest + " does not exist in the JAR file");
                    }
                    Files.copy(file, dest, StandardCopyOption.REPLACE_EXISTING);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

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
}
