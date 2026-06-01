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

package pascal.taie.frontend.java.syntax;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import pascal.taie.Main;
import pascal.taie.World;
import pascal.taie.ir.IR;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for Java syntax features across language versions supported
 * by the Java frontend.
 */
public class JavaVersionSyntaxTest {

    private static final Path RESOURCE_DIR = Path.of(
            "src", "test", "resources", "frontend", "java", "syntax");

    @BeforeEach
    void setUp() {
        World.reset();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("syntaxCases")
    void buildIRForVersionSyntax(SyntaxCase syntaxCase) {
        Path source = RESOURCE_DIR.resolve(syntaxCase.className() + ".java");
        assertTrue(Files.isRegularFile(source),
                "Test source does not exist: " + source);

        Main.buildWorld(
                "-java", Integer.toString(syntaxCase.frontendJavaVersion()),
                "-cp", RESOURCE_DIR.toString(),
                "--input-classes", syntaxCase.className());

        assertIRsCanBeBuilt(syntaxCase.className());
    }

    private static Stream<SyntaxCase> syntaxCases() {
        return Stream.of(
                // JDK 17+ javac no longer supports --release 6. This case
                // still exercises Java 6-compatible source syntax through
                // Tai-e's .java frontend path.
                new SyntaxCase(6, 8, "Java6Syntax"),
                new SyntaxCase(8, 8, "Java8Syntax"),
                new SyntaxCase(11, 11, "Java11Syntax"),
                new SyntaxCase(17, 17, "Java17Syntax"));
    }

    private static void assertIRsCanBeBuilt(String topLevelClassName) {
        ClassHierarchy hierarchy = World.get().getClassHierarchy();
        assertNotNull(hierarchy.getClass(topLevelClassName),
                "Input class is not in class hierarchy: " + topLevelClassName);

        List<JClass> appClasses = hierarchy.applicationClasses().toList();
        assertFalse(appClasses.isEmpty(),
                "No application classes found for " + topLevelClassName);

        for (JClass appClass : appClasses) {
            for (JMethod method : appClass.getDeclaredMethods()) {
                if (!method.isAbstract() && !method.isNative()) {
                    IR ir = method.getIR();
                    assertNotNull(ir, "IR should not be null for " + method);
                    assertFalse(ir.getStmts().isEmpty(),
                            "IR should not be empty for " + method);
                }
            }
        }
    }

    private record SyntaxCase(int syntaxJavaVersion, int frontendJavaVersion,
                              String className) {

        @Override
        public String toString() {
            return "Java " + syntaxJavaVersion + " syntax";
        }
    }
}
