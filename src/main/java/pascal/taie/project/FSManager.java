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

// TODO: close all fileSystem at right time
public class FSManager {
    Map<Path, FileSystem> fsMap;

    private FSManager() {
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

        Path p = path.resolve("jrt-fs.jar");
        if (Files.exists(p)) {
            URLClassLoader loader = new URLClassLoader(new URL[] { p.toUri().toURL() });
            FileSystem fs = FileSystems.newFileSystem(URI.create("jrt:/"),
                    Map.of("java.home", path.getParent().toString()), loader);
            fsMap.put(path, fs);
            return fs;
        } else {
            throw new FileNotFoundException("jrt-fs.jar not found in your jre dir");
        }
    }

    static FSManager manager;
    static public FSManager get() {
        if (manager == null) {
            manager = new FSManager();
        }
        return manager;
    }
}
