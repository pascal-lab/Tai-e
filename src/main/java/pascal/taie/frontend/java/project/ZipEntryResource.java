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

package pascal.taie.frontend.java.project;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

public class ZipEntryResource implements Resource {

    private final String path;

    private final FileSystem fileSystem;

    @Nullable
    private byte[] cache;

    public ZipEntryResource(String path, FileSystem fileSystem, @Nullable byte[] cache) {
        this.cache = cache;
        this.path = path;
        this.fileSystem = fileSystem;
    }

    @Override
    public Path getPath() {
        return fileSystem.getPath(path);
    }

    @Override
    public byte[] getContent() throws IOException {
        if (cache == null) {
            // NOTE: if reach here, this resource must be on the disk
            cache = Files.readAllBytes(fileSystem.getPath(path));
        }
        return cache;
    }

    @Override
    public void release() {
        cache = null;
    }
}
