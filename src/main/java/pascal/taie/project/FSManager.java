package pascal.taie.project;

import pascal.taie.util.collection.Maps;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
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

    static FSManager manager;
    static public FSManager get() {
        if (manager == null) {
            manager = new FSManager();
        }
        return manager;
    }
}
