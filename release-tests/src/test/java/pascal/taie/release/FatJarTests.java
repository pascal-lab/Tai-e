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

package pascal.taie.release;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class FatJarTests extends InvocationTests {

    @Override
    CliRunner createRunner(File workingDir) {
        String jarPath = System.getProperty("tai-e.fatjar.path");
        assertNotNull(jarPath, "System property 'tai-e.fatjar.path'"
                + " must be set to the path of the fat JAR");
        String javaPath = System.getProperty("java.executable.path");
        assertNotNull(javaPath, "System property 'java.executable.path'"
                + " must be set to the path of the Java executable");
        return CliRunner.forJar(javaPath, jarPath, workingDir);
    }

}
