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

import pascal.taie.util.collection.Maps;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Singleton class for managing file systems.
 * It provides methods to create and retrieve
 * file systems for zip files and the jrt file system.
 *
 * @see <a href="https://docs.oracle.com/javase/8/docs/technotes/guides/jlink/spec/jrtfs.html">JRT File System</a>
 */
public class FileSystemManager {
    Map<Path, FileSystem> fsMap;

    private FileSystemManager() {
        fsMap = Maps.newMap();
    }

    public FileSystem newZipFS(Path path) throws IOException {
        if (fsMap.containsKey(path)) {
            return fsMap.get(path);
        } else {
            FileSystem fs = FileSystems.newFileSystem(path);
            fsMap.put(path, fs);
            return fs;
        }
    }

    public FileSystem getJrtFs(Path path) throws IOException {
        if (fsMap.containsKey(path)) {
            return fsMap.get(path);
        }

        Path p = path.resolve("lib/jrt-fs.jar");
        if (Files.exists(p)) {
            URLClassLoader loader = new URLClassLoader(new URL[] { p.toUri().toURL() });
            FileSystem fs = FileSystems.newFileSystem(URI.create("jrt:/"),
                    Map.of("java.home", path.toString()), loader);
            fsMap.put(path, fs);
            return fs;
        } else {
            throw new FileNotFoundException("jrt-fs.jar not found in your jre dir");
        }
    }

    static FileSystemManager manager;
    public static FileSystemManager get() {
        if (manager == null) {
            manager = new FileSystemManager();
        }
        return manager;
    }
}
