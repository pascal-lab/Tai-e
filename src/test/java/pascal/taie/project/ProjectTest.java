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
import pascal.taie.config.Options;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;

public class ProjectTest {

    @Test
    void testLoadJar() throws IOException {
        FileLoader loader = FileLoader.get();
        String classes = "src/test/resources/world/classes.jar";
        List<FileContainer> cs = loader.loadRootContainers(List.of(Path.of(classes)));
        assertEquals(1, cs.size());

        FileContainer c = cs.get(0);
        for (var i : c.getFiles()) {
            if (i.getFileName().equals("Cards.class")) {
                assertSame(i.getRootContainer(), c);
                String cards = "src/test/resources/world/Cards.class";
                assertArrayEquals(i.getResource().getContent(),
                        Files.readAllBytes(Path.of(cards)));
                return;
            }
        }
        fail();
    }

    @Test
    void testLoadDir() throws IOException {
        Options options = Options.parse("-cp", "src/test/java");
        ProjectBuilder builder = new OptionsProjectBuilder(options);
        Project project = builder.build();
        ClassIndex index = project.makeIndex();
        ClassFile file = index.find("pascal.taie.project.ProjectTest");
        assertNotNull(file);
        try (InputStream in = new FileInputStream(
                "src/test/java/pascal/taie/project/ProjectTest.java")) {
            assertArrayEquals(in.readAllBytes(), file.getResource().getContent());
        }
    }

    private static final String CLASS = "A";

    private static final int RUN_TIMES =  10;

    @TempDir
    Path tempDir;

    @Test
    void testLoadDuplicateClass() throws IOException {
        String sourceCodeA1 = "public class A { public void methodA() {} }";
        String sourceCodeA2 = "public class A { public void methodB() {} }";

        Path a1 = tempDir.resolve("a1.jar");
        Path a2 = tempDir.resolve("a2.jar");

        compileAndCreateJar(a1, sourceCodeA1);
        compileAndCreateJar(a2, sourceCodeA2);
        int aCount = 0;
        for (int i = 0; i < RUN_TIMES; i++) {
            int result = runOnce(a1, a2);
            if (result == 0) {
                aCount++;
            } else if (result != 1) {
                Assertions.fail("Unexpected result: " + result);
            }
        }
        Assertions.assertEquals(RUN_TIMES, aCount);
    }

    private void compileAndCreateJar(Path jarPath, String src)
            throws IOException {
        // Write source file
        Path sourcePath = tempDir.resolve(CLASS + ".java");
        Files.writeString(sourcePath, src);

        // Compile the source file
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        compiler.run(null, null, null, sourcePath.toString());

        // Create JAR with the compiled class
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");

        Path classPath = tempDir.resolve(CLASS + ".class");
        try (FileOutputStream fos = new FileOutputStream(jarPath.toFile());
             JarOutputStream jos = new JarOutputStream(fos, manifest)) {

            JarEntry entry = new JarEntry(CLASS + ".class");
            jos.putNextEntry(entry);
            jos.write(Files.readAllBytes(classPath));
            jos.closeEntry();
        }
    }

    private static int runOnce(Path a1, Path a2) {
        World.reset();
        Main.buildWorld("-cp", a1 + File.pathSeparator + a2, "-m", CLASS);
        World world = World.get();
        JClass classA = world.getClassHierarchy().getClass(CLASS);
        Assertions.assertNotNull(classA);
        JMethod methodA = classA.getDeclaredMethod("methodA");
        JMethod methodB = classA.getDeclaredMethod("methodB");
        if (methodA != null) {
            return 0;
        } else if (methodB != null) {
            return 1;
        } else {
            return -1;
        }
    }
}
