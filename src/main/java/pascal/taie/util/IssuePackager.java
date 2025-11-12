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

import pascal.taie.config.Options;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class IssuePackager {
    public static void createIssuePackage(Options options) {
        try {
            List<Source> filePaths = new ArrayList<>();
            filePaths.add(new Source("output", new File(options.getOutputDir().getCanonicalPath())));
            options.getClassPath().stream()
                    .map(path -> new Source("cp", new File(path)))
                    .forEach(filePaths::add);
            options.getAppClassPath().stream()
                    .map(path -> new Source("acp", new File(path)))
                    .forEach(filePaths::add);
            options.getInputClasses().stream()
                    .map(path -> new Source("input-classes", new File(path)))
                    .forEach(filePaths::add);
            createZipFile("package.zip", filePaths);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void createZipFile(String zipFileName, List<Source> filePaths) {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFileName))) {
            for (Source source : filePaths) {
                File file = source.file;
                String category = source.category;
                addToZipFile(category, file, zos);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void addToZipFile(String category, File file, ZipOutputStream zos) {
        if (file.isDirectory()) {
            for (File subFile : Objects.requireNonNull(file.listFiles())) {
                addToZipFile(category + "/" + subFile.getName(), subFile, zos);
            }
        } else {
            try (FileInputStream fis = new FileInputStream(file)) {
                ZipEntry zipEntry = new ZipEntry(category + "/" + file.getName());
                zos.putNextEntry(zipEntry);

                byte[] buffer = new byte[1024];
                int length;
                while ((length = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, length);
                }
                zos.closeEntry();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private record Source(String category, File file) {
    }
}
