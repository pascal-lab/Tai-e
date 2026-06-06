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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocsTests {

    @Test
    @DisplayName("Should produce documentation ZIP")
    void testDocsZip() throws IOException {
        String docsZipPath = System.getProperty("tai-e.docs-zip.path");
        assertNotNull(docsZipPath, "System property 'tai-e.docs-zip.path'"
                + " must be set to the Tai-e documentation ZIP");
        String projectVersion = System.getProperty("tai-e.project.version");
        assertNotNull(projectVersion, "System property 'tai-e.project.version'"
                + " must be set to the Tai-e project version");

        Path docsZip = Path.of(docsZipPath);
        assertTrue(Files.isRegularFile(docsZip),
                "Documentation ZIP should exist: " + docsZip);
        assertTrue(Files.size(docsZip) > 0,
                "Documentation ZIP should not be empty");

        try (ZipFile zip = new ZipFile(docsZip.toFile())) {
            assertEntry(zip, projectVersion + "/reference/en/index.html");
            assertEntry(zip, projectVersion + "/api/index.html");
            if (Boolean.getBoolean("tai-e.project.snapshot")) {
                assertEntry(zip, "current/reference/en/index.html");
                assertEntry(zip, "current/api/index.html");
            }
        }
    }

    private static void assertEntry(ZipFile zip, String name) {
        assertNotNull(zip.getEntry(name),
                "Documentation ZIP should contain " + name);
    }

}
