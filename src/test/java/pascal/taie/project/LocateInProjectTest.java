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

import org.junit.jupiter.api.Test;
import pascal.taie.config.Options;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class LocateInProjectTest {

    //String singleClassPath = "src/test/resources/world/src.zip";
    private String classPath;

    private String javaFileToFind;

    private String className;

    Project createProject(String classPath) {
        Options options = Options.parse("-cp", classPath);
        ProjectBuilder builder = new OptionsProjectBuilder(options);
        return builder.build();
    }

    private void setupTest(String[] configs) {
        classPath = configs[0];
        javaFileToFind = configs[1];
        className = configs[2];
    }

    private final String[] testAll = {"src/test/java",
            "src/test/java/pascal/taie/project/LocateInProjectTest.java",
            "pascal.taie.project.LocateInProjectTest"};

    @Test
    void testLocate1() throws IOException {
        setupTest(testAll);
        Project project = createProject(classPath);
        assertNotNull(project);

        ProgramFile f = project.locate(className);
        assertNotNull(f);
        try (InputStream in = new FileInputStream(javaFileToFind)) {
            assertArrayEquals(in.readAllBytes(), f.getResource().getContent());
        }
    }
}
