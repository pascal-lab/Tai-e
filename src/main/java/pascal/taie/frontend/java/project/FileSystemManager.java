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

import pascal.taie.util.collection.Maps;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Map;

/**
 * Provides methods to retrieves zip and jrt file systems.
 *
 * @see <a href="https://docs.oracle.com/javase/8/docs/technotes/guides/jlink/spec/jrtfs.html">JRT File System</a>
 */
class FileSystemManager {

    /**
     * Cache for created file systems.
     */
    private static final Map<Path, FileSystem> cache = Maps.newMap();

    static FileSystem getZipFileSys(Path path) throws IOException {
        FileSystem fileSys = cache.get(path);
        if (fileSys == null) {
            fileSys = FileSystems.newFileSystem(path);
            cache.put(path, fileSys);
        }
        return fileSys;
    }

    static FileSystem getJrtFileSys(Path modules, Path jrtfs) throws IOException {
        FileSystem fileSys = cache.get(modules);
        if (fileSys == null) {
            URLClassLoader loader = new URLClassLoader(new URL[] { jrtfs.toUri().toURL() });
            fileSys = FileSystems.newFileSystem(URI.create("jrt:/"),
                    Map.of("java.home", modules.toString()), loader);
            cache.put(modules, fileSys);
        }
        return fileSys;
    }
}
