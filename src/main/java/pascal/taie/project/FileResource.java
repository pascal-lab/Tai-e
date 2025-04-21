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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * A <em>FileResource</em> is a file that can be read from the file system.
 *
 * @see Resource
 */
public class FileResource implements Resource {

    private final Path path;
    private byte[] readCache;

    public FileResource(Path path) {
        this.path = path;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (readCache != null) {
            return new ByteArrayInputStream(readCache);
        } else {
            return Files.newInputStream(path, StandardOpenOption.READ);
        }
    }

    @Override
    public byte[] getContent() throws IOException {
        if (readCache == null) {
            readCache = Files.readAllBytes(path);
        }
        return readCache;
    }

    @Override
    public Path getPath() {
        return path;
    }

    @Override
    public void release() {
        readCache = null;
    }
}
