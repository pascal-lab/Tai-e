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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DistributionTests extends InvocationTests {

    @Override
    CliRunner createRunner(File workingDir) throws IOException {
        String distributionZipPath = System.getProperty("tai-e.distribution-zip.path");
        assertNotNull(distributionZipPath, "System property "
                + "'tai-e.distribution-zip.path' must be set to the Tai-e "
                + "distribution ZIP");
        Path distributionDir = workingDir.toPath().resolve("distribution");
        extractZip(Path.of(distributionZipPath), distributionDir);
        Path executable = findExecutable(distributionDir);
        if (!isWindows()) {
            assertTrue(executable.toFile().setExecutable(true)
                            || executable.toFile().canExecute(),
                    "Tai-e distribution executable should be executable");
        }
        return CliRunner.forDistribution(executable.toString(), workingDir);
    }

    private static void extractZip(Path zipPath, Path outputDir) throws IOException {
        assertTrue(Files.isRegularFile(zipPath),
                "Distribution ZIP should exist: " + zipPath);
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                Path outputPath = outputDir.resolve(entry.getName()).normalize();
                if (!outputPath.startsWith(outputDir)) {
                    throw new IOException("Invalid ZIP entry: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(outputPath);
                } else {
                    Files.createDirectories(outputPath.getParent());
                    Files.copy(zip, outputPath, StandardCopyOption.REPLACE_EXISTING);
                }
                zip.closeEntry();
            }
        }
    }

    private static Path findExecutable(Path distributionDir) throws IOException {
        String executableName = isWindows() ? "tai-e.bat" : "tai-e";
        try (var files = Files.walk(distributionDir)) {
            return files
                    .filter(path -> path.getFileName().toString().equals(executableName))
                    .filter(path -> path.getParent() != null)
                    .filter(path -> path.getParent().getFileName().toString().equals("bin"))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError(
                            "Distribution ZIP should contain bin/" + executableName));
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }

}
