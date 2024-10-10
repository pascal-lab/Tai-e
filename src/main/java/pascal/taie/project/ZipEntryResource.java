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

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

public class ZipEntryResource implements Resource {

    @Nullable
    private byte[] cache;

    private final Path parent;

    private final String path;

    private final FileSystem fs;

    public ZipEntryResource(Path parent, byte[] cache, String path, FileSystem fs) {
        this.parent = parent;
        this.cache = cache;
        this.path = path;
        this.fs = fs;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (cache != null) {
            return new ByteArrayInputStream(cache);
        } else {
            // note: if reach here, parent must be on the disk
            try (FileSystem fs = FileSystems.newFileSystem(parent)) {
                return Files.newInputStream(fs.getPath(path));
            }
        }
    }

    @Override
    public byte[] getContent() throws IOException {
        if (cache == null) {
            // note: if reach here, parent must be on the disk
            cache = Files.readAllBytes(fs.getPath(path));
        }
        return cache;
    }

    @Override
    public Path getPath() {
        return fs.getPath(path);
    }

    @Override
    public void release() {
        cache = null;
    }
}
