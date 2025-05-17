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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pascal.taie.Main;
import pascal.taie.World;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public class DeterministicTest {
    @TempDir
    Path tempDir;

    private static final int RUN_TIMES =  10;

    private void compileAndCreateJar(Path jarPath, String className, String sourceCode) throws IOException {
        // Write source file
        Path sourcePath = tempDir.resolve(className + ".java");
        Files.writeString(sourcePath, sourceCode);

        // Compile the source file
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        compiler.run(null, null, null, sourcePath.toString());

        // Create JAR with the compiled class
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");

        Path classPath = tempDir.resolve(className + ".class");
        try (FileOutputStream fos = new FileOutputStream(jarPath.toFile());
             JarOutputStream jos = new JarOutputStream(fos, manifest)) {

            JarEntry entry = new JarEntry(className + ".class");
            jos.putNextEntry(entry);
            jos.write(Files.readAllBytes(classPath));
            jos.closeEntry();
        }
    }

    @Test
    public void test() throws IOException {
        String sourceCodeA = "public class A { public void methodA() {} }";
        String sourceCodeB = "public class A { public void methodB() {} }";

        Path aJar = tempDir.resolve("a.jar");
        Path bJar = tempDir.resolve("b.jar");

        compileAndCreateJar(aJar, "A", sourceCodeA);
        compileAndCreateJar(bJar, "A", sourceCodeB);
        int aCount = 0;
        for (int i = 0; i < RUN_TIMES; i++) {
            int result = runOnce(aJar, bJar);
            if (result == 0) {
                aCount++;
            } else if (result != 1) {
                Assertions.fail("Unexpected result: " + result);
            }
        }

        Assertions.assertEquals(RUN_TIMES, aCount);
    }

    int runOnce(Path a, Path b) {
        World.reset();
        Main.buildWorld("-cp", a + File.pathSeparator + b,
                "-m", "A");
        World w = World.get();
        JClass c = w.getClassHierarchy().getClass("A");
        Assertions.assertNotNull(c);
        JMethod methodA = c.getDeclaredMethod("methodA");
        JMethod methodB = c.getDeclaredMethod("methodB");
        if (methodA != null) {
            return 0;
        } else if (methodB != null) {
            return 1;
        } else {
            return -1;
        }
    }
}
